/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.edu.cedia.redi.elsevier.execute;

import ec.edu.cedia.redi.latindex.repository.Redi;
import ec.edu.cedia.redi.latindex.repository.RediRepository;
import ec.edu.cedia.redi.scopus.journals.JournalMetrics;
import java.util.Optional;
import java.util.Set;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cedia
 */
public class harvestJournals {

    private static final Logger log = LoggerFactory.getLogger(harvestJournals.class);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws RepositoryException, Exception {
        try (RediRepository r = RediRepository.getInstance()) {
            JournalMetrics metrics = new JournalMetrics();
            Redi redi = new Redi(r);
            Set<String> issnSet = redi.getISSNSet();
            ValueFactoryImpl instance = ValueFactoryImpl.getInstance();
            int i = 0;
            for (String ai : issnSet) {
                i++;
//                if (i<1558){
//                    continue;
//                }
                log.info("Processing : {} / {} ISSN: {}", i, issnSet.size(), ai);
                Model journal = metrics.getJournal(ai);
                Repository repo = new SailRepository(new MemoryStore());
                repo.initialize();
                RepositoryConnection connection = repo.getConnection();
                connection.add(journal);
                String q = "select distinct ?c { \n"
                        + "	?a <prism:eIssn>|<prism:issn> ?c .\n"
                        + "}";
                TupleQueryResult evaluate = connection.prepareTupleQuery(QueryLanguage.SPARQL, q).evaluate();
                boolean save = true;
                while (evaluate.hasNext()) {
                    BindingSet next = evaluate.next();
                    String stringValue = next.getValue("c").stringValue();
                    boolean ask = redi.ask("ask { graph <" + Redi.ELSEVIER_CONTEXT + "> { ?a <prism:eIssn>|<prism:issn> '" + stringValue + "'^^xsd:string } } ");
                    save = !ask;
                    Optional<String> image = metrics.getJournalImage(stringValue);
                    if (image.isPresent()) {
                        URI createURI = instance.createURI(image.get());
                        URI foafimg = instance.createURI("http://xmlns.com/foaf/0.1/img");
                        journal.add(instance.createBNode(), foafimg, createURI);
                    }
                }
                connection.close();
                repo.shutDown();
                if (save) {
                    redi.addModel(Redi.ELSEVIER_CONTEXT, journal);
                }
            }
        }
    }

}
