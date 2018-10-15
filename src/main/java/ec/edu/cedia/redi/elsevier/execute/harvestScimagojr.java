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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openrdf.model.BNode;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cedia
 */
public class harvestScimagojr {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(harvestScimagojr.class);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws RepositoryException, Exception {
        try (RediRepository r = RediRepository.getInstance()) {
            Redi redi = new Redi(r);
            List<Map<String, Value>> query = redi.query("PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"
                    + "select ?j ?i { \n"
                    + "	graph <" + Redi.ELSEVIER_CONTEXT + "> { \n"
                    + "		?un <prism:url> ?j . \n"
                    + "		?un dc:source-id ?i . \n"
                    + "	}\n"
                    + "}");
            int ix = 0;
            for (Map<String, Value> t : query) {
                String j = t.get("j").stringValue();
                String id = t.get("i").stringValue();
                boolean ask = redi.ask("ask from <" + Redi.SCIMAGOJR_CONTEXT + "> { <" + j + "> ?b ?c . }");
                log.info("Processing : {} / {} URI: {} ID: {}", ix, query.size(), j, id);
                if (!ask) {
                    Model scimagoInfo = getScimagoInfo(id, j);
                    redi.addModel(Redi.SCIMAGOJR_CONTEXT, scimagoInfo);
                }
                ix++;
            }
        }

    }

    public static Model getScimagoInfo(String ID, String URI) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        final ValueFactoryImpl instance = ValueFactoryImpl.getInstance();
        URI createURI = instance.createURI(URI);
        final Model m = new LinkedHashModel();
        String text = null;
        String textURL = null;
        String textCountry = null;
        String textScope = null;
        Model m2 = new LinkedHashModel();
        Document get = null;
        try {
            final String URL = "https://www.scimagojr.com/journalsearch.php?q=" + ID + "&tip=sid&clean=0";
            textURL = URL;
            get = Jsoup.connect(URL).get();
            text = get.select(".hindexnumber").text();
            Element firstDes = get.select(".journaldescription table tbody").first();
            for (Element t : firstDes.children()) {
                if (t.child(0).text().compareTo("Country") == 0) {
                    textCountry = t.child(1).text();
                }
                if (t.child(0).text().compareTo("Scope") == 0) {
                    textScope = t.child(1).text();
                }
            }

            Element first = get.select(".cell2x1 .cellslide table tbody").first();
            first.children().forEach((t) -> {
                BNode createBNode = instance.createBNode();
                String text1 = t.child(0).text();
                String text2 = t.child(1).text();
                String text3 = t.child(2).text();
                m2.add(createBNode,
                        instance.createURI("http://ucuenca.edu.ec/ontology#category"),
                        instance.createLiteral(text1));
                m2.add(createBNode,
                        instance.createURI("http://ucuenca.edu.ec/ontology#year"),
                        instance.createLiteral(Integer.parseInt(text2)));
                m2.add(createBNode,
                        instance.createURI("http://ucuenca.edu.ec/ontology#quartile"),
                        instance.createLiteral(text3));
                m2.add(createURI,
                        instance.createURI("http://ucuenca.edu.ec/ontology#Quartiles"),
                        createBNode);
                m.addAll(m2);
            });
            
            Element first2 = get.select(".cell1x1 .cellslide table tbody").first();
            first2.children().forEach((t) -> {
                BNode createBNode = instance.createBNode();
                String text1 = t.child(0).text();
                String text2 = t.child(1).text();
                m2.add(createBNode,
                        instance.createURI("http://ucuenca.edu.ec/ontology#year"),
                        instance.createLiteral(Integer.parseInt(text1)));
                m2.add(createBNode,
                        instance.createURI("http://ucuenca.edu.ec/ontology#SJR"),
                        instance.createLiteral(Double.parseDouble(text2)));
                m2.add(createURI,
                        instance.createURI("http://ucuenca.edu.ec/ontology#SJRs"),
                        createBNode);
                m.addAll(m2);
            });
            

        } catch (Exception ex) {
            Logger.getLogger(harvestScimagojr.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (textCountry != null && !textCountry.isEmpty()) {
            m.add(createURI,
                    instance.createURI("http://ucuenca.edu.ec/ontology#country"),
                    instance.createLiteral(textCountry));
        }
        if (textScope != null && !textScope.isEmpty()) {
            m.add(createURI,
                    instance.createURI("http://ucuenca.edu.ec/ontology#scope"),
                    instance.createLiteral(textScope));
        }

        if (text != null && !text.isEmpty()) {
            m.add(createURI,
                    instance.createURI("http://ucuenca.edu.ec/ontology#hindex"),
                    instance.createLiteral(Integer.parseInt(text)));
            m.add(createURI,
                    instance.createURI("http://purl.org/ontology/bibo/uri"),
                    instance.createURI(textURL));
        }
        if (!m2.isEmpty()) {
            Repository repo = new SailRepository(new MemoryStore());
            repo.initialize();
            RepositoryConnection connection = repo.getConnection();
            connection.add(m2);
            TupleQueryResult evaluate = connection.prepareTupleQuery(QueryLanguage.SPARQL, "select (min(?qu) as ?bq) {\n"
                    + "	{\n"
                    + "		select (max(?y) as ?ye) {\n"
                    + "			?a <http://ucuenca.edu.ec/ontology#Quartiles> ?q .\n"
                    + "			?q <http://ucuenca.edu.ec/ontology#year> ?y .\n"
                    + "		}\n"
                    + "	} .\n"
                    + "	{\n"
                    + "		select ?ye ?qu {\n"
                    + "			?a <http://ucuenca.edu.ec/ontology#Quartiles> ?q .\n"
                    + "			?q <http://ucuenca.edu.ec/ontology#year> ?ye .\n"
                    + "			?q <http://ucuenca.edu.ec/ontology#quartile> ?qu .\n"
                    + "		}\n"
                    + "	}\n"
                    + "}").evaluate();
            if (evaluate.hasNext()) {
                BindingSet next = evaluate.next();
                String stringValue = next.getValue("bq").stringValue();
                m.add(createURI,
                        instance.createURI("http://ucuenca.edu.ec/ontology#bestQuartile"),
                        instance.createLiteral(stringValue));
            }
            connection.close();
            repo.shutDown();
        }
        return m;
    }

}
