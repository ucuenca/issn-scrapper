/*
 * Copyright 2018 Xavier Sumba <xavier.sumba93@ucuenca.ec>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ec.edu.cedia.redi.issn.scrappe.latindex.redi;

import java.util.ArrayList;
import java.util.List;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public class Redi {

    private final RediRepository conn;
    private final ValueFactory vf = ValueFactoryImpl.getInstance();
    private static final Logger log = LoggerFactory.getLogger(Redi.class);
    private static final String PUB_TEST_CONTEXT = "https://redi.cedia.edu.ec/context/latindexTest";
    private static final String PUB_SCHOLAR_CONTEXT = "https://redi.cedia.edu.ec/context/provider/GoogleScholarProvider";
    private static final String PUB_SCIELO_CONTEXT = "https://redi.cedia.edu.ec/context/provider/ScieloProvider";
    private static final String LATINDEX_CONTEXT = "https://redi.cedia.edu.ec/context/latindex";

    public Redi(RediRepository conn) {
        this.conn = conn;
    }

    public List<Publication> getPublications() throws QueryEvaluationException {
        List<Publication> publications = new ArrayList<>();

        try {
            RepositoryConnection connection = conn.getConnection();
            String query = "PREFIX dct: <http://purl.org/dc/terms/>\n"
                    + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                    + "PREFIX bibo: <http://purl.org/ontology/bibo/>\n"
                    + "SELECT DISTINCT * WHERE { \n"
                    + "  GRAPH ?pubGraph {  \n"
                    + "    [] foaf:publications ?p\n"
                    + "  }\n"
                    + "  GRAPH ?dataGraph {  \n"
                    + "    ?p dct:title ?t.\n"
                    + "    OPTIONAL { ?p bibo:abstract ?a. }\n"
                    + "  }\n"
                    + "}";

            TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
            q.setBinding("pubGraph", vf.createURI(PUB_TEST_CONTEXT));
            q.setBinding("dataGraph", vf.createURI(PUB_SCHOLAR_CONTEXT));
            TupleQueryResult result = q.evaluate();

            while (result.hasNext()) {
                BindingSet variables = result.next();
                Publication p = new Publication();
                p.setUri(variables.getBinding("p").getValue().stringValue());
                p.setUri(variables.getBinding("t").getValue().stringValue());
                p.setUri(variables.getBinding("a").getValue().stringValue());

                publications.add(p);
            }

            q.setBinding("dataGraph", vf.createURI(PUB_SCIELO_CONTEXT));
            result = q.evaluate();

            while (result.hasNext()) {
                BindingSet variables = result.next();
                Publication p = new Publication();
                p.setUri(variables.getBinding("p").getValue().stringValue());
                p.setTitle(variables.getBinding("t").getValue().stringValue());
                p.setAbztract(variables.getBinding("a").getValue().stringValue());

                publications.add(p);
            }
            connection.close();
        } catch (RepositoryException | MalformedQueryException ex) {
            log.error("Cannot query publications", ex);
        }
        return publications;
    }

    public List<Issn> getIssns() throws QueryEvaluationException {
        List<Issn> issn = new ArrayList<>();

        try {
            RepositoryConnection connection = conn.getConnection();
            String query = "SELECT DISTINCT *"
                    + "FROM ?latindex"
                    + "WHERE {"
                    + "   ?s <http://www.ucuenca.edu.ec/ontology/issn> ?o"
                    + "}";

            TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
            q.setBinding("latindex", vf.createURI(LATINDEX_CONTEXT));
            TupleQueryResult result = q.evaluate();

            while (result.hasNext()) {
                BindingSet variables = result.next();
                Issn i = new Issn();
                i.setUri(variables.getBinding("s").getValue().stringValue());
                i.setIssn(variables.getBinding("o").getValue().stringValue());

                issn.add(i);
            }

            connection.close();
        } catch (RepositoryException | MalformedQueryException ex) {
            log.error("Cannot query latindex ISSNs", ex);
        }
        return issn;
    }

}
