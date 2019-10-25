/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.edu.cedia.redi.elsevier.execute;

import ec.edu.cedia.redi.graphdb.centralgraph.BoundedExecutor;
import ec.edu.cedia.redi.latindex.repository.Redi;
import ec.edu.cedia.redi.latindex.repository.RediRepository;
import java.util.List;
import java.util.Map;
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
public class linkJournalsAndBooks {

    private static final Logger log = LoggerFactory.getLogger(linkJournalsAndBooks.class);

    /**
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) throws RepositoryException, Exception {
        try (RediRepository r = RediRepository.getInstance()) {
            Redi redi = new Redi(r);
            final List<Map.Entry<String, String>> issnPubSet = redi.getISSNPubSet();
            int ix = 0;
            BoundedExecutor threadPool = BoundedExecutor.getThreadPool(2);
            for (final Map.Entry<String, String> en : issnPubSet) {
                final int ixx = ix;
                threadPool.submitTask(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            log.info("Processing : {} / {} ISSN: {}", ixx, issnPubSet.size(), en.getKey());
                            Set<String> issnElsevier = redi.getISSNorISBNElsevier(en.getValue());
                            for (String i : issnElsevier) {
                                redi.addSt(en.getKey(), "http://www.w3.org/2000/01/rdf-schema#seeAlso", i, Redi.ELSEVIER_CONTEXT + "SameAs");
                            }
                        } catch (Exception ex) {
                            java.util.logging.Logger.getLogger(linkJournalsAndBooks.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
                ix++;
            }
            threadPool.end();
            threadPool = BoundedExecutor.getThreadPool(2);
            final List<Map.Entry<String, String>> isbnPubSet = redi.getISBNPubSet();
            ix = 0;
            for (final Map.Entry<String, String> en : isbnPubSet) {
                final int ixx = ix;
                threadPool.submitTask(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            log.info("Processing : {} / {} ISBN: {}", ixx, isbnPubSet.size(), en.getKey());
                            Set<String> isbnElsevier = redi.getISSNorISBNElsevier(en.getValue());
                            for (String i : isbnElsevier) {
                                redi.addSt(en.getKey(), "http://www.w3.org/2000/01/rdf-schema#seeAlso", i, Redi.ELSEVIER_CONTEXT + "SameAs");
                            }
                        } catch (Exception ex) {
                            java.util.logging.Logger.getLogger(linkJournalsAndBooks.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                );
                ix++;
            }
            threadPool.end();
            redi.transformElsevierURL();
            redi.mergeElsevierJournals();
            redi.linkElsevierJournals();

        }
    }

}
