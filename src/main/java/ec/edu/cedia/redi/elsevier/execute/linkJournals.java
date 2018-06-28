/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.edu.cedia.redi.elsevier.execute;

import ec.edu.cedia.redi.latindex.repository.Redi;
import ec.edu.cedia.redi.latindex.repository.RediRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cedia
 */
public class linkJournals {

    private static final Logger log = LoggerFactory.getLogger(linkJournals.class);

    /**
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) throws RepositoryException, Exception {
        try (RediRepository r = RediRepository.getInstance()) {
            Redi redi = new Redi(r);
            List<Map.Entry<String, String>> issnPubSet = redi.getISSNPubSet();
            int ix = 0;
            for (Map.Entry<String, String> en : issnPubSet) {
                log.info("Processing : {} / {} ISSN: {}", ix, issnPubSet.size(), en.getKey());
                Set<String> issnElsevier = redi.getISSNElsevier(en.getValue());
                for (String i : issnElsevier) {
                    redi.addSt(en.getKey(), "http://www.w3.org/2000/01/rdf-schema#seeAlso", i, Redi.ELSEVIER_CONTEXT + "SameAs");
                }
                ix++;
            }
            redi.transformElsevierURL();
            redi.mergeElsevierJournals();
            redi.linkElsevierJournals();

        }
    }

}
