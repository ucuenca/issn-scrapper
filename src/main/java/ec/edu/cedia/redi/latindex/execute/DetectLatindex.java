/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.edu.cedia.redi.latindex.execute;

import com.google.common.base.Preconditions;
import ec.edu.cedia.redi.latindex.api.Query;
import ec.edu.cedia.redi.latindex.api.WebSearcher;
import ec.edu.cedia.redi.latindex.model.Issn;
import ec.edu.cedia.redi.latindex.model.Journal;
import ec.edu.cedia.redi.latindex.model.Publication;
import ec.edu.cedia.redi.latindex.repository.Redi;
import ec.edu.cedia.redi.latindex.repository.RediRepository;
import ec.edu.cedia.redi.latindex.search.BingSearch;
import ec.edu.cedia.redi.latindex.search.query.StrictQuery;
import ec.edu.cedia.redi.latindex.search.query.Value;
import ec.edu.cedia.redi.latindex.utils.ModifiedJaccard;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
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

    public static void main(String[] args) throws InterruptedException {
        do {
            try {
                main2(null);
                break;
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(DetectLatindex.class.getName()).log(Level.SEVERE, null, ex);
                Thread.sleep(5 * 1000);
            }
        } while (true);
    }

    private static int last = 170800;

    public static void main2(String[] args) throws RepositoryException, Exception {
        try (RediRepository r = RediRepository.getInstance()) {
            Redi redi = new Redi(r);
            Map<String, Journal> latindexJournals = redi.getLatindexJournals();
            for (int i = last; i < 10000000; i += 10) {
                last = i;
                List<Publication> publications = redi.getPublications(i);

                int n = 0;
                for (Publication p : publications) {
                    n++;
                    System.out.println(String.format("%s + %s / %s", n + "", i + "", "" + publications.size()));
                    String w = "http://mock.com/" + (getMD5(p.toString2()));
                    if (isValidURI(redi, p.getUri()) && !hasLink(redi, "SameAsJournals", p.getUri(), null)) {
                        if (!hasLink(redi, "Processed", p.getUri(), w)) {
                            Set<String> results = new HashSet<>();
                            if (p.getOissn() != null) {
                                results.addAll(processISSN(p, redi));
                            } else {
                                if (results.isEmpty()) {
                                    if (p.getOjournal() != null) {
                                        results.addAll(processJournalName(p, redi, latindexJournals));
                                    } else if (results.isEmpty()) {
                                        results.addAll(processWeb(p, redi, latindexJournals));
                                    }
                                }
                            }
                            for (String res : results) {
                                addLink(redi, "SameAsJournals", p.getUri(), res);
                            }
                            addLink(redi, "Processed", p.getUri(), w);
                        }
                    }

                }
            }
        }
    }

    public static Set<String> processWeb(Publication p, Redi r, Map<String, Journal> jols) throws RepositoryException, InterruptedException {
        Set<String> latJournals = new HashSet<>();
        FindPotentialIssn f = new FindPotentialIssn(r, new BingSearch());
        f.findPotentialIssn(p);
        Map<String, Double> pesos = new HashMap<>();
        if (!p.getIssnPerPage().isEmpty()) {
            Set<String> issns = new HashSet<>();
            for (List<Issn> l : p.getIssnPerPage().values()) {
                for (Issn i : l) {
                    issns.add(i.getIssn());
                }
            }
            if (!issns.isEmpty()) {
                for (String aissn : issns) {
                    double df = 0;
                    double N = p.getIssnPerPage().values().size();
                    for (List<Issn> l : p.getIssnPerPage().values()) {
                        boolean contains = false;
                        for (Issn i : l) {
                            if (i.getIssn().equals(aissn)) {
                                contains = true;
                                break;
                            }
                        }
                        if (contains) {
                            df++;
                        }
                    }
                    double sum = 0;
                    for (List<Issn> l : p.getIssnPerPage().values()) {
                        double nt = l.size();
                        double tf = 0;
                        for (Issn i : l) {
                            if (i.getIssn().equals(aissn)) {
                                tf++;
                            }
                        }
                        double s = nt > 0 ? (tf * df) / (nt) : 0;
                        sum += s;

                    }
                    double res = N > 0 ? sum / N : 0;
                    pesos.put(aissn, res);
                }
                for (Map.Entry<String, Double> en : pesos.entrySet()) {
                    if (en.getValue() >= 0.5) {
                        String uri = null;
                        for (Issn in : p.getIssn()) {
                            if (in.getIssn().equals(en.getKey())) {
                                uri = in.getUri();
                            }
                        }
                        Journal get = jols.get(uri);
                        if (webValidation(p, get, 1)) {
                            latJournals.add(uri);
                        }
                    }
                }
            }
        }
        return latJournals;
    }

    public static boolean isValidURI(Redi r, String uri) throws RepositoryException {
        return r.isValidURI(uri);
    }

    public static boolean hasLink(Redi r, String suff, String p, String j) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        return r.hasSt(p, "http://www.w3.org/2000/01/rdf-schema#seeAlso", j, Redi.LATINDEX_CONTEXT + suff);
    }

    public static void addLink(Redi r, String suff, String p, String j) throws RepositoryException {
        r.addSt(p, "http://www.w3.org/2000/01/rdf-schema#seeAlso", j, Redi.LATINDEX_CONTEXT + suff);
    }

    public static Set<String> processJournalName(Publication p, Redi r, Map<String, Journal> latindexJournals) throws QueryEvaluationException, RepositoryException, MalformedQueryException, InterruptedException {
        Set<String> latJournals = new HashSet<>();
        if (p.getOjournal() != null) {
            for (Journal onePossibleJournal : latindexJournals.values()) {
                if (JournalNameComparison(p.getOjournal(), onePossibleJournal.getName())) {
                    if (webValidation(p, onePossibleJournal, 1) || webValidation(p, onePossibleJournal, 2)) {
                        latJournals.add(onePossibleJournal.getURI());
                    }
                }
            }
        }
        return latJournals;
    }

    public static Set<String> processISSN(Publication p, Redi r) throws QueryEvaluationException, RepositoryException {
        Set<String> latJournals = new HashSet<>();
        if (p.getOissn() != null) {
            List<String> latindexJournalByISSN = r.getLatindexJournalByISSN(p.getOissn(), false);
            latindexJournalByISSN.addAll(r.getLatindexJournalByISSN(p.getOissn(), true));
            for (String latId : latindexJournalByISSN) {
                latJournals.add(latId);
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

    public static String getMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            String hashtext = number.toString(16);
            // Now we need to zero pad it if you actually want the full 32 chars.
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
