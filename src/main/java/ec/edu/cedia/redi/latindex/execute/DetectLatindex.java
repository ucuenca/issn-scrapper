/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.edu.cedia.redi.latindex.execute;

import com.google.common.base.Preconditions;
import ec.edu.cedia.redi.latindex.api.Query;
import ec.edu.cedia.redi.latindex.api.WebSearcher;
import ec.edu.cedia.redi.latindex.model.Journal;
import ec.edu.cedia.redi.latindex.model.Publication;
import ec.edu.cedia.redi.latindex.repository.Redi;
import ec.edu.cedia.redi.latindex.repository.RediRepository;
import ec.edu.cedia.redi.latindex.search.query.StrictQuery;
import ec.edu.cedia.redi.latindex.search.query.Value;
import ec.edu.cedia.redi.latindex.utils.BoundedExecutor;
import ec.edu.cedia.redi.latindex.utils.ModifiedJaccard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cedia
 */
public class DetectLatindex {

    private static final Logger log = LoggerFactory.getLogger(DetectLatindex.class);

    public static boolean webValidation(Publication p, Journal j, int lvl) throws InterruptedException {
        Preconditions.checkArgument(lvl > 0 && lvl < 3);
        boolean thowEx = false;

        Query query = null;
        String title = p.getTitle();
        String abztract = p.getAbztract();
        String issn = j.getISSN();
        String journal = j.getName();

        // Build query based on lvl1 or lvl2. Abstractis optional.
        switch (lvl) {
            case 1:
                if (abztract == null) {
                    query = new StrictQuery(new Value(title, 50), new Value(journal, 50), new Value(issn, -1));
                } else {
                    query = new StrictQuery(new Value(title, 25), new Value(abztract, 50), new Value(journal, 25), new Value(issn, -1));
                }
                break;
            case 2:
                if (abztract == null) {
                    query = new StrictQuery(new Value(title, 100), new Value(issn, -1));
                } else {
                    query = new StrictQuery(new Value(title, 25), new Value(abztract, 75), new Value(issn, -1));
                }
                break;
        }

        // Select a web browser available. If the search fails, try with another 
        // browsers. If there isn't a browser available, trow an exception.
        List<String> urls = Collections.emptyList();
        ServiceLoader<WebSearcher> loader = ServiceLoader.load(WebSearcher.class);
        RuntimeException ex = null;
        for (WebSearcher webSearcher : loader) {
            try {
                urls = webSearcher.getUrls(query, 1);
                thowEx = false;
                break;
            } catch (RuntimeException e) {
                ex = e;
                thowEx = true;
            }
        }

        if (ex != null && thowEx) {
            throw ex;
        }

        return !urls.isEmpty();
    }

    //Etapa 2: Validar
    public static void main(String[] args) throws RepositoryException, Exception {
        try (RediRepository r = RediRepository.getInstance()) {
            Redi redi = new Redi(r);
            List<Map.Entry<String, String>> stage2Candidates = redi.getStage2Candidates();
            List<Publication> publications = redi.getPublications2();
            Map<String, Journal> latindexJournals = redi.getLatindexJournals2();

            for (Publication p : publications) {
                for (Map.Entry<String, String> s2 : stage2Candidates) {
                    if (p.getUri().equals(s2.getKey())) {
                        Journal get = latindexJournals.get(s2.getValue());
                        for (int lvl = 1; lvl <= 2; lvl++) {
                            if (webValidation(p, get, lvl)) {
                                addLink(redi, "ValidatedStage2", p.getUri(), get.getURI());
                                break;
                            }
                        }
                    }
                }

            }

        }
    }

    /// Etapa 1 ISSN, Etapa 2: Comparar nombres poner en 
    public static void main_resp(String[] args) throws RepositoryException, Exception {
        try (RediRepository r = RediRepository.getInstance()) {
            Redi redi = new Redi(r);
            List<Publication> publications = redi.getPublications();
            Map<String, Journal> latindexJournals = redi.getLatindexJournals();
            BoundedExecutor threadPool = BoundedExecutor.getThreadPool(2);
            int n = 0;
            for (Publication p : publications) {
                n++;
                final int nn = n;
                threadPool.submitTask(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //Stage 1 - ISSN
                            Set<String> processISSN = processISSN(p, redi);
                            if (processISSN.isEmpty()) {
                                //Stage 2 - Name
                                Set<String> processJournalName = processJournalName(p, redi, latindexJournals);

                                //q1
                                String q1 = "\"" + p.getTitle() + "\" ";

                            }
                            System.out.println("Fin " + nn);
                            //Stage 3
                            //FindPotentialIssn findPotentialIssn = new FindPotentialIssn(redi,new GoogleSearch());
                            //findPotentialIssn.findPotentialIssn(p);
                        } catch (QueryEvaluationException ex) {
                            java.util.logging.Logger.getLogger(DetectLatindex.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (RepositoryException ex) {
                            java.util.logging.Logger.getLogger(DetectLatindex.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (MalformedQueryException ex) {
                            java.util.logging.Logger.getLogger(DetectLatindex.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
            }
            threadPool.end();
        }
    }

    public static void addLink(Redi r, String suff, String p, String j) throws RepositoryException {
        r.addSt(p, "http://www.w3.org/2000/01/rdf-schema#seeAlso", j, Redi.LATINDEX_CONTEXT + suff);
    }

    public static Set<String> processJournalName(Publication p, Redi r, Map<String, Journal> latindexJournals) throws QueryEvaluationException, RepositoryException, MalformedQueryException {
        Set<String> latJournals = new HashSet<>();
        if (p.getOjournal() != null) {
            List<Journal> CandidatesList = new ArrayList<>();
            for (Journal onePossibleJournal : latindexJournals.values()) {
                if (JournalNameComparison(p.getOjournal(), onePossibleJournal.getName())) {
                    CandidatesList.add(onePossibleJournal);
                    addLink(r, "SameAsCandidates1", p.getUri(), onePossibleJournal.getURI());
                }
            }
        }
        return latJournals;
    }

    public static Set<String> processISSN(Publication p, Redi r) throws QueryEvaluationException, RepositoryException {
        Set<String> latJournals = new HashSet<>();
        if (p.getOissn() != null) {
            List<String> latindexJournalByISSN = r.getLatindexJournalByISSN(p.getOissn());
            for (String latId : latindexJournalByISSN) {
                latJournals.add(latId);
                addLink(r, "SameAsPublications1", p.getUri(), latId);
            }
        }
        return latJournals;
    }

    public static boolean JournalNameComparison(String NameJournal1, String NameJournal2) {
        ModifiedJaccard SynDistance = new ModifiedJaccard();
        double DistanceJournalName = SynDistance.distanceJournalName(NameJournal1, NameJournal2);
        return DistanceJournalName > 0.9;
    }

    public static int getFirstNStringsLen(String str, int n) {
        ModifiedJaccard mod = new ModifiedJaccard();
        List<String> tokenizer = mod.tokenizer(str);
        List<String> subList = tokenizer.subList(0, n < tokenizer.size() ? n : tokenizer.size());
        return subList.size();
    }

    public static String getFirstNStrings(String str, int n) {
        ModifiedJaccard mod = new ModifiedJaccard();
        List<String> tokenizer = mod.tokenizer(str);
        List<String> subList = tokenizer.subList(0, n < tokenizer.size() ? n : tokenizer.size());
        return String.join(" ", subList);
    }

}
