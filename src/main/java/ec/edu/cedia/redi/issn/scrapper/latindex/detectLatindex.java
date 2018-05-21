/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.edu.cedia.redi.issn.scrapper.latindex;

import ec.edu.cedia.redi.issn.scrappe.redi.repository.Redi;
import ec.edu.cedia.redi.issn.scrappe.redi.repository.RediRepository;
import ec.edu.cedia.redi.issn.scrapper.model.Journal;
import ec.edu.cedia.redi.issn.scrapper.model.Publication;
import ec.edu.cedia.redi.issn.scrapper.search.GoogleSearch;
import ec.edu.cedia.redi.issn.scrapper.search.WebSearcher;
import ec.edu.cedia.redi.issn.scrapper.utils.BoundedExecutor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
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
public class detectLatindex {

    private static final Logger log = LoggerFactory.getLogger(detectLatindex.class);

    final static String q1 = "%s %s %s %s"; // title + abstract + issn + journal
    final static String q2 = "%s %s %s"; // title + abstract + issn
    final static String q3 = "%s %s %s"; // title + abstract + journal
    final static List<String> queries = new ArrayList<>();

    static {
        queries.add(q1);
        queries.add(q2);
        //queries.add(q3);
    }

    public static boolean webValidation(Publication p, Journal j) throws InterruptedException {

        WebSearcher search = new GoogleSearch();

        String title = p.getTitle();
        String abztract = p.getAbztract();
        String issn = j.getISSN();
        String jorunal = j.getName();
        if (abztract == null) {
            System.out.println("no");
            return false;
        }

        int tot = 32;
        String e1 = getFirstNStrings(title, 10);
        tot -= getFirstNStringsLen(title, 10);
        String e2 = getFirstNStrings(abztract, 15);
        tot -= getFirstNStringsLen(abztract, 15);
        String e3 = issn;
        tot -= 1;
        String e4 = getFirstNStrings(jorunal, tot);

        String q = String.format("\"%s\" \"%s\" \"%s\" \"%s\"", e1, e2, e3, e4);

        System.out.println(q);
        int randomNum = ThreadLocalRandom.current().nextInt(8, 15 + 1);
        Thread.sleep(randomNum*1000);
        List<String> urls = search.getUrls(q, 1);
        if (!urls.isEmpty()) {
            System.out.println("ok");
            return true;
        }
        System.out.println("no");
        return false;
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
                        if (webValidation(p, get)) {
                            addLink(redi, "ValidatedStage2", p.getUri(), get.getURI());
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
                            java.util.logging.Logger.getLogger(detectLatindex.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (RepositoryException ex) {
                            java.util.logging.Logger.getLogger(detectLatindex.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (MalformedQueryException ex) {
                            java.util.logging.Logger.getLogger(detectLatindex.class.getName()).log(Level.SEVERE, null, ex);
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
