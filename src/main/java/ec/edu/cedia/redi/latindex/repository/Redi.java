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
package ec.edu.cedia.redi.latindex.repository;

import ec.edu.cedia.redi.latindex.model.Issn;
import ec.edu.cedia.redi.latindex.model.Journal;
import ec.edu.cedia.redi.latindex.model.Publication;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.query.Binding;
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
    public static final String PUB_CONTEXT = "https://redi.cedia.edu.ec/context/redi";
    public static final String LATINDEX_CONTEXT = "https://redi.cedia.edu.ec/context/latindex";
    public static final String POTENTIAL_ISSN_CONTEXT = "https://redi.cedia.edu.ec/context/latindexPotentialIssn";
    public static final String AUGMENT_ISSN_INFO_CONTEXT = "https://redi.cedia.edu.ec/context/latindexAugmentInfo";
    public static final String ROAD_ISSN_CONTEXT = "https://redi.cedia.edu.ec/context/roadissn";

    public static final String UC_PREFIX = "http://www.ucuenca.edu.ec/ontology/";

    public Redi(RediRepository conn) {
        this.conn = conn;
    }

    public void addSt(String s, String p, String o, String c) throws RepositoryException {
        RepositoryConnection connection = this.conn.getConnection();
        connection.begin();
        URI su = vf.createURI(s);
        URI pu = vf.createURI(p);
        URI ou = vf.createURI(o);
        URI cu = vf.createURI(c);
        connection.add(su, pu, ou, cu);
        connection.commit();
        connection.close();

    }

    public List<String> getLatindexJournalByISSN(String issn) throws QueryEvaluationException {
        List<String> ls = new ArrayList<>();
        try {
            RepositoryConnection connection = conn.getConnection();
            String q = "SELECT distinct ?a WHERE { \n"
                    + "  values ?g { <https://redi.cedia.edu.ec/context/latindex> <https://redi.cedia.edu.ec/context/latindexAugmentInfo>}\n"
                    + "  GRAPH ?g {  \n"
                    + "    ?a <http://www.ucuenca.edu.ec/ontology/issn> ?c .\n"
                    + "  }\n"
                    + "}";
            TupleQuery prepareTupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, q);
            prepareTupleQuery.setBinding("c", vf.createLiteral(issn));
            TupleQueryResult evaluate = prepareTupleQuery.evaluate();
            while (evaluate.hasNext()) {
                BindingSet next = evaluate.next();
                ls.add(next.getValue("a").stringValue());
            }
            connection.close();
        } catch (RepositoryException | MalformedQueryException ex) {
            log.error("Cannot query publications", ex);

        }
        return ls;
    }

    public List<Publication> getPublications(int off) throws QueryEvaluationException {
        List<Publication> publications = new ArrayList<>();

        try {
            RepositoryConnection connection = conn.getConnection();
            String query = "PREFIX dct: <http://purl.org/dc/terms/>\n"
                    + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                    + "PREFIX bibo: <http://purl.org/ontology/bibo/>\n"
                    + "SELECT DISTINCT * WHERE { \n"
                    //+ "  GRAPH ?pubGraph {  \n"
                    //+ "    [] foaf:publications ?p\n"
                    //+ "  }\n"
                    + "  GRAPH ?dataGraph {  \n"
                    //+ "    [] foaf:publications ?p .\n"
                    + "    ?p dct:title ?t.\n"
                    + "    OPTIONAL { ?p bibo:abstract ?a. }\n"
                    + "    OPTIONAL { ?p bibo:issn ?i. }\n"
                    + "    OPTIONAL { ?p dct:isPartOf ?j. }\n"
                    + "    OPTIONAL { ?j rdfs:label ?jl. }\n"
                    + "  }\n"
                    + "} offset " + off + " limit 1000";

            TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
            //q.setBinding("pubGraph", vf.createURI(PUB_CONTEXT));
            getPublicationsGraph(q, publications, PUB_CONTEXT);

            connection.close();
        } catch (RepositoryException | MalformedQueryException ex) {
            log.error("Cannot query publications", ex);
        }
        return publications;
    }

    private void getPublicationsGraph(TupleQuery q, List<Publication> publications, String graph) throws QueryEvaluationException {
        q.setBinding("dataGraph", vf.createURI(graph));
        log.info("Getting publications {}...", graph);
        TupleQueryResult result = q.evaluate();

        while (result.hasNext()) {
            BindingSet variables = result.next();
            Publication p = new Publication();
            if (!variables.hasBinding("p")) {
                continue;
            }
            p.setUri(variables.getBinding("p").getValue().stringValue());
            p.setTitle(variables.getBinding("t").getValue().stringValue());
            if (variables.hasBinding("a")) {
                p.setAbztract(variables.getBinding("a").getValue().stringValue());
            }
            if (variables.hasBinding("i")) {
                p.setOissn(variables.getBinding("i").getValue().stringValue());
            }
            if (variables.hasBinding("jl")) {
                p.setOjournal(variables.getBinding("jl").getValue().stringValue());
            }

            publications.add(p);
        }
    }

    public void storePublicationAllPotentialIssn(Publication p) {
        RepositoryConnection connection = null;
        try {
            connection = conn.getConnection();
            connection.begin();
            URI publication = vf.createURI(p.getUri());
            if (p.getIssn().isEmpty()) {
                Statement stm = vf.createStatement(publication, vf.createURI(UC_PREFIX + "hasIssn"), vf.createLiteral(false));
                connection.add(stm, vf.createURI(POTENTIAL_ISSN_CONTEXT));
            } else {
                Statement stm = vf.createStatement(publication, vf.createURI(UC_PREFIX + "hasIssn"), vf.createLiteral(true));
                connection.add(stm, vf.createURI(POTENTIAL_ISSN_CONTEXT));
            }
            for (Issn i : p.getIssn()) {
                URI issn = vf.createURI(i.getUri());
                Statement stm = vf.createStatement(publication, vf.createURI(UC_PREFIX + "potentialIssn"), issn);
                connection.add(stm, vf.createURI(POTENTIAL_ISSN_CONTEXT));
            }

            for (Map.Entry<String, List<Issn>> entry : p.getIssnPerPage().entrySet()) {
                URI webpage = vf.createURI(entry.getKey());
                for (Issn issn : entry.getValue()) {
                    Statement pubWeb = vf.createStatement(publication, vf.createURI(UC_PREFIX, "hasWebResults"), webpage);
                    Statement webIssn = vf.createStatement(webpage, vf.createURI(UC_PREFIX, "hasIssn"), vf.createURI(issn.getUri()));
                    Statement issnWeb = vf.createStatement(vf.createURI(issn.getUri()), vf.createURI(UC_PREFIX, "belongsTo"), webpage);

                    connection.add(pubWeb, vf.createURI(POTENTIAL_ISSN_CONTEXT));
                    connection.add(webIssn, vf.createURI(POTENTIAL_ISSN_CONTEXT));
                    connection.add(issnWeb, vf.createURI(POTENTIAL_ISSN_CONTEXT));
                }
            }
            connection.commit();
        } catch (RepositoryException ex) {
        } finally {
            try {
                connection.close();
            } catch (RepositoryException ex) {
            }
        }

    }

    public List<Issn> getIssns() throws QueryEvaluationException {
        List<Issn> issn = new ArrayList<>();

        try {
            RepositoryConnection connection = conn.getConnection();
            String query = "PREFIX uc: <http://www.ucuenca.edu.ec/ontology/>"
                    + "SELECT DISTINCT * WHERE { GRAPH ?latindex {"
                    + "   ?s uc:issn ?o. }}";

            TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
            q.setBinding("latindex", vf.createURI(LATINDEX_CONTEXT));
            log.info("Getting ISSN info...");
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

    public Issn getIssn(String issn) throws RepositoryException {
        RepositoryConnection connection = conn.getConnection();
        try {
            String query = "PREFIX uc: <http://www.ucuenca.edu.ec/ontology/>\n"
                    + "SELECT DISTINCT ?uri\n"
                    + "WHERE {\n"
                    + "  GRAPH ?latindex {\n"
                    + "	     ?uri uc:issn ?o1.\n"
                    + "  }\n"
                    + "  GRAPH ?latindexAugment {\n"
                    + "      ?uri uc:issn ?o2.\n"
                    + "  }\n"
                    + "  FILTER (regex(str(?o1), str(?issn), \"i\") || regex(str(?o2), str(?issn), \"i\")) \n"
                    + "} LIMIT 1";

            TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query);
            q.setBinding("latindex", vf.createURI(LATINDEX_CONTEXT));
            q.setBinding("latindexAugment", vf.createURI(AUGMENT_ISSN_INFO_CONTEXT));
            q.setBinding("issn", vf.createLiteral(issn));
            log.info("Searching ({}) ISSN in repository.", issn);
            TupleQueryResult result = q.evaluate();

            if (result.hasNext()) {
                Issn i = new Issn();
                BindingSet variables = result.next();
                i.setUri(variables.getBinding("uri").getValue().stringValue());
                i.setIssn(issn);

                return i;
            }

        } catch (MalformedQueryException | QueryEvaluationException ex) {
            log.error("Cannot query latindex ISSNs", ex);
        } finally {
            connection.close();
        }
        return null;
    }

    public Issn getRoadIssn(String issn) throws RepositoryException {
        RepositoryConnection connection = conn.getConnection();
        try {
            String findRoot = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                    + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                    + "PREFIX frbroo: <http://issn.org/ns/fr/frbr/frbroo/>\n"
                    + "PREFIX cidoc: <http://www.cidoc-crm.org/cidoc-crm/>\n"
                    + "\n"
                    + "SELECT ?root\n"
                    + "WHERE {\n"
                    + "  GRAPH ?road {\n"
                    + "    ?root a frbroo:F18_Serial_Work.\n"
                    + "    ?issn rdf:value \"%s\".\n"
                    + "    {\n"
                    + "      ?issn ^cidoc:P1_is_identified_by/^frbroo:R10i_is_member_of ?root.\n"
                    + "    } UNION {\n"
                    + "      ?issn ^cidoc:P1_is_identified_by ?root.\n"
                    + "    }\n"
                    + "  }   \n"
                    + "}";
            findRoot = String.format(findRoot, issn);

            TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, findRoot);
            q.setBinding("road", vf.createURI(ROAD_ISSN_CONTEXT));
            log.info("Searching ({}) resource ISSN in repository.", issn);
            TupleQueryResult result = q.evaluate();

            Issn i = null;
            if (result.hasNext()) {
                i = new Issn();
                BindingSet variables = result.next();
                i.setUri(variables.getBinding("root").getValue().stringValue());
            }

            if (i == null) {
                return null;
            }

            String findInfo = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
                    + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                    + "PREFIX frbroo: <http://issn.org/ns/fr/frbr/frbroo/>\n"
                    + "PREFIX cidoc: <http://www.cidoc-crm.org/cidoc-crm/>\n"
                    + "\n"
                    + "SELECT ?value\n"
                    + "WHERE {\n"
                    + "  GRAPH ?road {\n"
                    + "  	?target a frbroo:F13_Identifier.\n"
                    + "	{?issn frbroo:R10i_is_member_of/cidoc:P1_is_identified_by ?target.}\n"
                    + "    UNION    \n"
                    + "    {?issn cidoc:P1_is_identified_by ?target.}   \n"
                    + "    ?target rdf:value ?value\n"
                    + "  }   \n"
                    + "}";
            q = connection.prepareTupleQuery(QueryLanguage.SPARQL, findInfo);
            q.setBinding("road", vf.createURI(ROAD_ISSN_CONTEXT));
            q.setBinding("issn", vf.createURI(i.getUri()));

            log.info("Searching values for resource ({}) in repository.", i.getUri());
            result = q.evaluate();
            while (result.hasNext()) {
                BindingSet variables = result.next();
                String alternativeIssn = variables.getBinding("value").getValue().stringValue();
                if (!alternativeIssn.equalsIgnoreCase(issn)) {
                    i.setIssn(alternativeIssn);
                    return i;
                }
            }
        } catch (MalformedQueryException | QueryEvaluationException ex) {
            log.error("Cannot query latindex ISSNs", ex);
        } finally {
            connection.close();
        }
        return null;
    }

    public boolean hasPubPotentialIssn(Publication p) throws RepositoryException {
        RepositoryConnection connection = conn.getConnection();
        return connection.hasStatement(vf.createURI(p.getUri()),
                vf.createURI(UC_PREFIX + "hasIssn"), null, true, vf.createURI(POTENTIAL_ISSN_CONTEXT));

    }

    public void augmentIssn(Issn latIssn, Issn roadIssn) {
        RepositoryConnection connection = null;
        try {
            connection = conn.getConnection();
            connection.begin();

            URI uriLatIndex = vf.createURI(latIssn.getUri());
            Statement stm = vf.createStatement(uriLatIndex, vf.createURI(UC_PREFIX + "issn"), vf.createLiteral(roadIssn.getIssn()));
            connection.add(stm, vf.createURI(AUGMENT_ISSN_INFO_CONTEXT));
            stm = vf.createStatement(uriLatIndex, OWL.SAMEAS, vf.createURI(roadIssn.getUri()));
            connection.add(stm, vf.createURI(AUGMENT_ISSN_INFO_CONTEXT));

            connection.commit();
        } catch (RepositoryException ex) {
        } finally {
            try {
                connection.close();
            } catch (RepositoryException ex) {
            }
        }
    }

    public List<Map<String, Value>> query(String q) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        List<Map<String, Value>> r = new ArrayList<>();
        RepositoryConnection connection = conn.getConnection();
        TupleQueryResult evaluate = connection.prepareTupleQuery(QueryLanguage.SPARQL, q).evaluate();
        while (evaluate.hasNext()) {
            Iterator<Binding> iterator = evaluate.next().iterator();
            Map<String, Value> mp = new HashMap<>();
            while (iterator.hasNext()) {
                Binding next = iterator.next();
                mp.put(next.getName(), next.getValue());
            }
            r.add(mp);
        }
        return r;
    }

    public List<Map.Entry<String, String>> getStage2Candidates() throws RepositoryException, MalformedQueryException, QueryEvaluationException {

        List<Map.Entry<String, String>> ls = new ArrayList<>();
        String q = "select * {\n"
                + "	graph <" + Redi.LATINDEX_CONTEXT + "SameAsCandidates1> {\n"
                + "    	?a ?b ?c .\n"
                + "    }\n"
                + "}";
        List<Map<String, Value>> allLatindexJournals = query(q);
        for (Map<String, Value> a : allLatindexJournals) {
            Map.Entry<String, String> t = new AbstractMap.SimpleEntry<>(a.get("a").stringValue(), a.get("c").stringValue());
            ls.add(t);

        }

        return ls;
    }

    public Map<String, Journal> getLatindexJournals() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        //Extracting Latindex journals
        String q = "SELECT DISTINCT ?JOURNAL ?NAME ?TOPIC ?YEAR ?ISSN { "
                + " GRAPH <" + Redi.LATINDEX_CONTEXT + "> {   "
                + "?JOURNAL a <http://www.ucuenca.edu.ec/ontology/journal> . "
                + "?JOURNAL <http://www.ucuenca.edu.ec/ontology/tit_clave> ?NAME ."
                + "?JOURNAL <http://www.ucuenca.edu.ec/ontology/subtema> ?TOPIC ."
                + "?JOURNAL <http://www.ucuenca.edu.ec/ontology/ano_ini> ?YEAR ."
                + "?JOURNAL <http://www.ucuenca.edu.ec/ontology/issn> ?ISSN ."
                + "} "
                + "}";

        List<Map<String, Value>> allLatindexJournals = query(q);
        Map< String, Journal> allLatindexJournalsObjects = new HashMap<>();
        for (Map<String, Value> aLatindexJournal : allLatindexJournals) {
            String JournalURI = aLatindexJournal.get("JOURNAL").stringValue();
            String JournalName = aLatindexJournal.get("NAME").stringValue().replaceAll("\\(.*?\\)", " ").trim();
            String JournalTopic = aLatindexJournal.get("TOPIC").stringValue();
            String JournalYear = aLatindexJournal.get("YEAR").stringValue();
            String JournalISSN = aLatindexJournal.get("ISSN").stringValue();

            int JournalYearInt = 0;
            try {
                JournalYearInt = Integer.parseInt(JournalYear);
            } catch (Exception ex) {
                log.warn("Invalid year {} in the central graph publication {}.", JournalYear, JournalURI);
            }

            if (allLatindexJournalsObjects.containsKey(JournalURI)) {
                allLatindexJournalsObjects.get(JournalURI).getTopics().add(JournalTopic);
            } else {
                Journal newLatindexJournal = new Journal(JournalURI, JournalName, JournalISSN, new ArrayList<String>(), JournalYearInt);
                newLatindexJournal.getTopics().add(JournalTopic);
                allLatindexJournalsObjects.put(JournalURI, newLatindexJournal);
            }
        }
        return allLatindexJournalsObjects;
    }

    public boolean hasSt(String s, String p, String o, String c) throws RepositoryException {
        boolean t = false;
        RepositoryConnection connection = this.conn.getConnection();
        connection.begin();
        URI su = vf.createURI(s);
        URI pu = vf.createURI(p);
        URI ou = o != null ? vf.createURI(o) : null;
        URI cu = vf.createURI(c);
        t = connection.hasStatement(su, pu, ou, false, cu);
        connection.commit();
        connection.close();
        return t;
    }
}
