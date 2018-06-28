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
public class harvestBooks {

    private static final Logger log = LoggerFactory.getLogger(harvestJournals.class);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws RepositoryException, Exception {
        try (RediRepository r = RediRepository.getInstance()) {
            JournalMetrics metrics = new JournalMetrics();
            Redi redi = new Redi(r);
            Set<String> isbnSet = redi.getISBNSet();
            ValueFactoryImpl instance = ValueFactoryImpl.getInstance();
            int i = 0;
            for (String ai : isbnSet) {
                log.info("Processing : {} / {} ISBN: {}", i, isbnSet.size(), ai);
                Model book = metrics.getBook(ai);
                Repository repo = new SailRepository(new MemoryStore());
                repo.initialize();
                RepositoryConnection connection = repo.getConnection();
                connection.add(book);
                String q = "select distinct ?c { \n"
                        + "	?a <prism:isbn> ?c .\n"
                        + "}";
                TupleQueryResult evaluate = connection.prepareTupleQuery(QueryLanguage.SPARQL, q).evaluate();
                while (evaluate.hasNext()) {
                    BindingSet next = evaluate.next();
                    String stringValue = next.getValue("c").stringValue();
                    Optional<String> image = metrics.getBookImage(stringValue);
                    if (image.isPresent()) {
                        URI createURI = instance.createURI(image.get());
                        URI foafimg = instance.createURI("http://xmlns.com/foaf/0.1/img");
                        book.add(instance.createBNode(), foafimg, createURI);
                    }
                }
                connection.close();
                repo.shutDown();
                redi.addModel(Redi.ELSEVIER_CONTEXT, book);
                i++;
            }
        }
    }

}
