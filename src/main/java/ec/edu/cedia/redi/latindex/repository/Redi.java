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
import ec.edu.cedia.redi.latindex.utils.HTTPUtils;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openrdf.model.BNode;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.UpdateExecutionException;
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
    public static final String BASE_CONTEXT = "https://redi.cedia.edu.ec/context/";
    public static final String PUB_CONTEXT = BASE_CONTEXT + "redix";
    public static final String LATINDEX_CONTEXT = BASE_CONTEXT + "latindex";
    public static final String POTENTIAL_ISSN_CONTEXT = BASE_CONTEXT + "latindexPotentialIssn";
    public static final String AUGMENT_ISSN_INFO_CONTEXT = BASE_CONTEXT + "latindexAugmentInfo";
    public static final String ROAD_ISSN_CONTEXT = BASE_CONTEXT + "roadissn";
    public static final String ELSEVIER_CONTEXT = BASE_CONTEXT + "elsevier";
    public static final String SCIMAGOJR_CONTEXT = BASE_CONTEXT + "scimagojr";
    public static final String UC_PREFIX = "http://www.ucuenca.edu.ec/ontology/";

    public Redi(RediRepository conn) {
        this.conn = conn;
    }

    public void addStBN(String s, String p, String o, String c) throws RepositoryException {
        RepositoryConnection connection = this.conn.getConnection();
        connection.begin();
        URI su = vf.createURI(s);
        URI pu = vf.createURI(p);
        URI ou = vf.createURI("_:", o);
        URI cu = vf.createURI(c);
        connection.add(su, pu, ou, cu);
        connection.commit();
        connection.close();

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

    public List<String> getLatindexJournalByISSN(String issn, boolean v) throws QueryEvaluationException {
        List<String> ls = new ArrayList<>();
        try {
            RepositoryConnection connection = conn.getConnection();
            String q = "SELECT distinct ?a WHERE { \n"
                    + "  values ?g { <https://redi.cedia.edu.ec/context/latindex> <https://redi.cedia.edu.ec/context/latindexAugmentInfo>}\n"
                    + "  GRAPH ?g {  \n"
                    + "    ?a <http://www.ucuenca.edu.ec/ontology/issn> ?o1 .\n"
                    + "    FILTER (regex(str(?o1), str(?issn), \"i\"))  \n"
                    + "  }\n"
                    + "}";
            TupleQuery prepareTupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, q);

            String var = issn.replaceAll("-", "").toLowerCase().trim();
            while (var.length() < 8) {
                var = "0" + var;
            }

            if (v) {
                var = var.substring(0, 4) + "-" + var.substring(4, var.length());
            }
            prepareTupleQuery.setBinding("issn", vf.createLiteral(var));
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

            String query_op = "PREFIX bibo: <http://purl.org/ontology/bibo/>\n"
                    + "select distinct ?p { graph <" + Redi.PUB_CONTEXT + "> { ?p a bibo:AcademicArticle } } offset " + off + " limit 5";
            List<Map<String, Value>> query2 = query(query_op);
            String qp = "";
            for (Map<String, Value> amv : query2) {
                String stringValue = amv.get("p").stringValue();
                if (isValidURI(stringValue)) {
                    qp += " <" + stringValue + "> ";
                }
            }

            String query = "PREFIX dct: <http://purl.org/dc/terms/>\n"
                    + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                    + "PREFIX bibo: <http://purl.org/ontology/bibo/>\n"
                    + "SELECT DISTINCT * WHERE { \n"
                    //+ "  GRAPH ?pubGraph {  \n"
                    //+ "    [] foaf:publications ?p\n"
                    //+ "  }\n"
                    + "  GRAPH ?dataGraph {  \n"
                    //+ "    [] foaf:publications ?p .\n"
                    + "    values ?p {" + qp + " } . \n"
                    + "    ?p dct:title ?t.\n"
                    + "    OPTIONAL { ?p bibo:abstract ?a. }\n"
                    + "    OPTIONAL { ?p bibo:issn ?i. }\n"
                    + "    OPTIONAL { ?p dct:isPartOf ?j. OPTIONAL { ?j rdfs:label ?jl. } }\n"
                    + "    \n"
                    + "  }\n"
                    + "} ";

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
        String q = "SELECT ?JOURNAL ?NAME ?TOPIC ?YEAR ?ISSN { "
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

    public boolean hasSt(String s, String p, String o, String c) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        boolean t = false;
        RepositoryConnection connection = this.conn.getConnection();
        connection.begin();
        BooleanQuery prepareBooleanQuery = null;

        if (o != null) {
            prepareBooleanQuery = connection.prepareBooleanQuery(QueryLanguage.SPARQL,
                    "ask from <" + c + "> { <" + s + "> <" + p + "> <" + o + "> }");
        } else {
            prepareBooleanQuery = connection.prepareBooleanQuery(QueryLanguage.SPARQL,
                    "ask from <" + c + "> { <" + s + "> <" + p + "> ?o }");
        }
        t = prepareBooleanQuery.evaluate();
        connection.commit();
        connection.close();
        return t;
    }

    public void removeEmptyCollections() throws RepositoryException, MalformedQueryException, UpdateExecutionException {
        String q = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX dct: <http://purl.org/dc/terms/>\n"
                + "delete {\n"
                + "	graph <" + PUB_CONTEXT + "> {\n"
                + "             ?c ?p ?v .\n"
                + "      	?s ?h ?c .\n"
                + "    }\n"
                + "} where {\n"
                + "	{\n"
                + "      select ?c {\n"
                + "          graph <" + PUB_CONTEXT + "> {\n"
                + "              [] dct:isPartOf ?c .\n"
                + "              optional {\n"
                + "                  ?c rdfs:label ?l .\n"
                + "                  bind ( strlen(?l) as ?len).\n"
                + "              }\n"
                + "          }\n"
                + "      } group by ?c having (max(?len) < 2)\n"
                + "    } .\n"
                + "  	graph <" + PUB_CONTEXT + "> {\n"
                + "             ?c ?p ?v .\n"
                + "      	?s ?h ?c .\n"
                + "    }\n"
                + "}";
        update(q);
    }

    public void updateOtherIndexes() throws RepositoryException, MalformedQueryException, UpdateExecutionException {
        String q = "PREFIX dct: <http://purl.org/dc/terms/>\n"
                + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "insert {\n"
                + "	graph <" + PUB_CONTEXT + "> {\n"
                + "    	?j <http://ucuenca.edu.ec/ontology#index> ?pr.\n"
                + "    }\n"
                + "}where {\n"
                + "	graph <" + PUB_CONTEXT + "> {\n"
                + "  		[] foaf:publications ?p .\n"
                + "      	values ?pr {<http://ucuenca.edu.ec/ontology#SpringerProvider>\n"
                + "                   <http://ucuenca.edu.ec/ontology#ScopusProvider>\n"
                + "                   <http://ucuenca.edu.ec/ontology#Scielo>\n"
                + "                   } .\n"
                + "      	?p dct:provenance ?pr .\n"
                + "      	?p dct:isPartOf ?j .\n"
                + "      	filter not exists { ?j <http://ucuenca.edu.ec/ontology#index> <http://ucuenca.edu.ec/ontology#Latindex> . }\n"
                + "    }\n"
                + "}";
        update(q);
    }

    public void updateLatindex() throws RepositoryException, MalformedQueryException, UpdateExecutionException {
        String q = "PREFIX dct: <http://purl.org/dc/terms/>\n"
                + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "PREFIX bibo: <http://purl.org/ontology/bibo/>\n"
                + "insert {\n"
                + "	graph <" + PUB_CONTEXT + "> {\n"
                + "    	?p dct:isPartOf ?j .\n"
                + "  		?j a bibo:Journal .\n"
                + "  		?j <http://www.w3.org/2000/01/rdf-schema#label> ?tit .\n"
                + "  		?j <http://ucuenca.edu.ec/ontology#index> <http://ucuenca.edu.ec/ontology#Latindex> .\n"
                + "    }\n"
                + "}where {\n"
                + "	graph <" + PUB_CONTEXT + "> {\n"
                + "  		[] foaf:publications ?p .\n"
                + "    }\n"
                + "    graph <" + LATINDEX_CONTEXT + "SameAsJournals> {\n"
                + "		?p <http://www.w3.org/2000/01/rdf-schema#seeAlso> ?j .\n"
                + "    }\n"
                + "    graph <" + LATINDEX_CONTEXT + "> {\n"
                + "		?j <http://www.ucuenca.edu.ec/ontology/tit_clave> ?tit .\n"
                + "    }\n"
                + "}";
        update(q);
        q = "PREFIX dct: <http://purl.org/dc/terms/>\n"
                + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "PREFIX bibo: <http://purl.org/ontology/bibo/>\n"
                + "insert {\n"
                + "	graph <" + PUB_CONTEXT + "> {\n"
                + "  		?j bibo:issn ?issn .\n"
                + "    }\n"
                + "}where {\n"
                + "    graph <" + LATINDEX_CONTEXT + "SameAsJournals> {\n"
                + "		?p <http://www.w3.org/2000/01/rdf-schema#seeAlso> ?j .\n"
                + "    }\n"
                + "    graph <" + LATINDEX_CONTEXT + "> {\n"
                + "		?j <http://www.ucuenca.edu.ec/ontology/issn> ?issn .\n"
                + "    }\n"
                + "}";
        update(q);
        q = "PREFIX dct: <http://purl.org/dc/terms/>\n"
                + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "PREFIX bibo: <http://purl.org/ontology/bibo/>\n"
                + "insert {\n"
                + "	graph <" + PUB_CONTEXT + "> {\n"
                + "  		?j <http://ucuenca.edu.ec/ontology#subject-area> ?sa .\n"
                + "    }\n"
                + "}where {\n"
                + "    graph <" + LATINDEX_CONTEXT + "SameAsJournals> {\n"
                + "		?p <http://www.w3.org/2000/01/rdf-schema#seeAlso> ?j .\n"
                + "    }\n"
                + "    graph <" + LATINDEX_CONTEXT + "> {\n"
                + "		?j <http://www.ucuenca.edu.ec/ontology/subtema> ?sa .\n"
                + "    }\n"
                + "}";
        update(q);
        q = "PREFIX dct: <http://purl.org/dc/terms/>\n"
                + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "PREFIX bibo: <http://purl.org/ontology/bibo/>\n"
                + "insert {\n"
                + "	graph <" + PUB_CONTEXT + "> {\n"
                + "  		?j bibo:uri ?uri .\n"
                + "    }\n"
                + "}where {\n"
                + "    graph <" + LATINDEX_CONTEXT + "SameAsJournals> {\n"
                + "		?p <http://www.w3.org/2000/01/rdf-schema#seeAlso> ?j .\n"
                + "    }\n"
                + "    graph <" + LATINDEX_CONTEXT + "> {\n"
                + "		?j <http://www.ucuenca.edu.ec/ontology/folio> ?f .\n"
                + "             bind (iri(concat('http://latindex.org/latindex/ficha?folio=', ?f)) as ?uri) .\n"
                + "    }\n"
                + "}";
        update(q);
        q = "PREFIX dct: <http://purl.org/dc/terms/>\n"
                + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "PREFIX bibo: <http://purl.org/ontology/bibo/>\n"
                + "insert {\n"
                + "	graph <" + PUB_CONTEXT + "> {\n"
                + "  		?j dct:publisher ?pub2uri .\n"
                + "             ?pub2uri a foaf:Organization .\n"
                + "             ?pub2uri foaf:name ?edi .\n"
                + "    }\n"
                + "}where {\n"
                + "    graph <" + LATINDEX_CONTEXT + "SameAsJournals> {\n"
                + "		?p <http://www.w3.org/2000/01/rdf-schema#seeAlso> ?j .\n"
                + "    }\n"
                + "    graph <" + LATINDEX_CONTEXT + "> {\n"
                + "		?j <http://www.ucuenca.edu.ec/ontology/nombre_edi> ?edi .\n"
                + "          	bind(encode_for_uri(str(?edi)) as ?pub2h) .\n"
                + "          	bind (iri(concat('https://redi.cedia.edu.ec/resource/publisher/',?pub2h)) as ?pub2uri) .\n"
                + "    }\n"
                + "}";
        update(q);
    }

    public void updateLatindexImg() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        String q = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX dct: <http://purl.org/dc/terms/>\n"
                + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "select distinct ?j ?f {\n"
                + "	graph <" + PUB_CONTEXT + "> {\n"
                + "      	?j <http://ucuenca.edu.ec/ontology#index> <http://ucuenca.edu.ec/ontology#Latindex> .\n"
                + "    }\n"
                + "    graph <" + LATINDEX_CONTEXT + "> {\n"
                + "      	?j <http://www.ucuenca.edu.ec/ontology/folio> ?f  .\n"
                + "    }\n"
                + "}";
        List<Map<String, Value>> query = query(q);

        for (Map<String, Value> mo : query) {
            log.info("Checking journal {} of {}", query.indexOf(mo), query.size());
            String j = mo.get("j").stringValue();
            String f = mo.get("f").stringValue();
            String ping = "http://www.latindex.org/lat/portadas/fotRev/" + f + ".jpg";
            if (HTTPUtils.pingURL(ping, 10 * 1000)) {
                addSt(j, "http://xmlns.com/foaf/0.1/img", ping, PUB_CONTEXT);
                log.info("Image {} found for journal {}", ping, j);
            }
        }
    }

    public boolean isValidURI(String uri) throws RepositoryException {
        boolean t = false;
        try {
            new java.net.URI(uri);
            t = true;
        } catch (Exception e) {
            log.info("Invalid URI {} ignoring", uri);
        }
        return t;
    }

    public Set<String> getISSNSet() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        String q = "select distinct ?p {\n"
                + " graph <" + PUB_CONTEXT + "> {\n"
                + "     [] <http://purl.org/ontology/bibo/issn> ?p .\n"
                + " }\n"
                + "}";
        List<Map<String, Value>> query = query(q);
        Set<String> hs = new HashSet<>();
        for (Map<String, Value> mp : query) {
            String issn = mp.get("p").stringValue();
            issn = issn.replaceAll("-", "").replaceAll("–", "").toLowerCase().trim();
            while (issn.length() < 8) {
                issn = "0" + issn;
            }
            hs.add(issn);
        }
        return hs;
    }

    public Set<String> getISBNSet() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        String q = "select distinct ?p {\n"
                + " graph <" + PUB_CONTEXT + "> {\n"
                + "     [] <http://purl.org/ontology/bibo/isbn> ?p .\n"
                + " }\n"
                + "}";
        List<Map<String, Value>> query = query(q);
        Set<String> hs = new HashSet<>();
        for (Map<String, Value> mp : query) {
            String issn = mp.get("p").stringValue();
            issn = issn.replaceAll("-", "").toLowerCase().trim();
            hs.add(issn);
        }
        return hs;
    }

    public List<Map.Entry<String, String>> getISBNPubSet() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        String q = "select distinct ?p ?i  {\n"
                + " graph <" + PUB_CONTEXT + "> {\n"
                + "     ?p <http://purl.org/ontology/bibo/isbn> ?n .\n"
                + "     bind(lcase(str(?n)) as ?i) .\n"
                + " }\n"
                + "}";
        List<Map<String, Value>> query = query(q);
        List<Map.Entry<String, String>> hs = new ArrayList<>();
        for (Map<String, Value> mp : query) {
            String pub = mp.get("p").stringValue();
            String issn = mp.get("i").stringValue();
            hs.add(new AbstractMap.SimpleEntry<>(pub, issn));
        }
        return hs;
    }

    public void addModel(String context, Model m) throws RepositoryException {
        RepositoryConnection connection = this.conn.getConnection();
        URI contextUri = connection.getValueFactory().createURI(context);
        connection.begin();
        connection.add(m, contextUri);
        connection.commit();
        connection.close();
    }

    public List<Map.Entry<String, String>> getISSNPubSet() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        String q = "select distinct ?p ?i  {\n"
                + " graph <" + PUB_CONTEXT + "> {\n"
                + "     ?p <http://purl.org/ontology/bibo/issn> ?n .\n"
                + "     bind(lcase(str(?n)) as ?i) .\n"
                + " }\n"
                + "}";
        List<Map<String, Value>> query = query(q);
        List<Map.Entry<String, String>> hs = new ArrayList<>();
        for (Map<String, Value> mp : query) {
            String pub = mp.get("p").stringValue();
            String issn = mp.get("i").stringValue();
            issn = issn.replaceAll("-", "").toLowerCase().trim();
            while (issn.length() < 8) {
                issn = "0" + issn;
            }
            issn = issn.substring(0, 4) + "-" + issn.substring(4, issn.length());
            hs.add(new AbstractMap.SimpleEntry<>(pub, issn.toLowerCase()));
        }
        return hs;
    }

    public Set<String> getISSNorISBNElsevier(String issnOrIsbn) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        String q = "select distinct ?u {\n"
                + "	graph <" + ELSEVIER_CONTEXT + "> {\n"
                + "      values ?p {<prism:eIssn> <prism:issn> <prism:isbn>} ."
                + "      ?a ?p ?i .\n"
                + "      ?a <prism:url> ?u .\n"
                + "      filter (lcase(str(?i)) ='" + issnOrIsbn.replaceAll("\n", "") + "') .\n"
                + "    }\n"
                + "}";
        List<Map<String, Value>> query = query(q);
        Set<String> hs = new HashSet<>();
        for (Map<String, Value> mp : query) {
            String stringValue = mp.get("u").stringValue();
            hs.add(stringValue);
        }
        return hs;
    }

    public void transformElsevierURL() throws RepositoryException, MalformedQueryException, UpdateExecutionException {
        String q = "delete {\n"
                + "    graph <" + ELSEVIER_CONTEXT + "> {\n"
                + "      ?un <prism:url> ?u .\n"
                + "    }\n"
                + "} insert {\n"
                + "    graph <" + ELSEVIER_CONTEXT + "> {\n"
                + "      ?un <prism:url> ?ur .\n"
                + "    }\n"
                + "} where {\n"
                + "	graph <" + ELSEVIER_CONTEXT + "> {\n"
                + "		?un <prism:url> ?u .\n"
                + "      	bind (iri(?u) as ?ur) .\n"
                + "      	filter (isLiteral(?u)) .\n"
                + "	}\n"
                + "} ";
        String q1 = "delete {\n"
                + "    graph <" + ELSEVIER_CONTEXT + "> {\n"
                + "      ?un <prism:url> ?u .\n"
                + "    }\n"
                + "} insert {\n"
                + "    graph <" + ELSEVIER_CONTEXT + "> {\n"
                + "      ?un <http://purl.org/dc/elements/1.1/@href> ?ur .\n"
                + "    }\n"
                + "} where {\n"
                + "	graph <" + ELSEVIER_CONTEXT + "> {\n"
                + "		?un <http://purl.org/dc/elements/1.1/@href> ?u .\n"
                + "      	bind (iri(?u) as ?ur) .\n"
                + "      	filter (isLiteral(?u)) .\n"
                + "	}\n"
                + "} ";
        String q2 = "insert {\n"
                + "    graph <" + ELSEVIER_CONTEXT + "> {\n"
                + "      ?un <http://xmlns.com/foaf/0.1/img2> ?i .\n"
                + "    }\n"
                + "} where {\n"
                + "	graph <" + ELSEVIER_CONTEXT + "> {\n"
                + "		[] <http://xmlns.com/foaf/0.1/img> ?i .\n"
                + "		?un <http://purl.org/dc/elements/1.1/link> ?l .\n"
                + "		?l <http://purl.org/dc/elements/1.1/@href> ?i .\n"
                + "	}\n"
                + "} ";
        update(q);
        update(q1);
        update(q2);
    }

    public void update(String q) throws RepositoryException, MalformedQueryException, UpdateExecutionException {
        RepositoryConnection connection = conn.getConnection();
        connection.begin();
        connection.prepareUpdate(QueryLanguage.SPARQL, q).execute();
        connection.commit();
        connection.close();
    }

    public String getListJournalsElsevierQuery(boolean issn, boolean onlyJournal) {
        String q = "select distinct ?j {\n"
                + "    graph <" + ELSEVIER_CONTEXT + "> {\n"
                + "      ?un <prism:url> ?j .\n";
        if (issn) {
            q += "      ?un <prism:issn>|<prism:eIssn> [] .\n"
                    + "      values ?s {'journal'^^<http://www.w3.org/2001/XMLSchema#string>}   "
                    + "      filter" + (onlyJournal ? " " : " not ") + "exists{\n"
                    + "      	?un <prism:aggregationType> ?s .	\n"
                    + "      }\n";
        } else {
            q = "select ?j {\n"
                    + "    graph <" + ELSEVIER_CONTEXT + "> {\n"
                    + "      ?un <prism:isbn> [] .\n"
                    + "      ?un <prism:url> ?j .\n";
        }
        q += "	}\n"
                + "} ";

        return q;
    }

    public String getUpdateElsevierQuery(String stringValue, String type) {
        String q2 = "PREFIX dct: <http://purl.org/dc/terms/>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + "insert  {\n"
                + "  graph <" + PUB_CONTEXT + "> {\n"
                + "	?is a ?ty .\n"
                + "	?is rdfs:label ?n .\n"
                + "  	?is foaf:img ?i .\n"
                + "  	?is <http://ucuenca.edu.ec/ontology#index>  <http://ucuenca.edu.ec/ontology#ScopusProvider> .\n"
                + "  	?is <http://ucuenca.edu.ec/ontology#edition> ?ed .\n"
                + "  	?is <http://ucuenca.edu.ec/ontology#subject-area> ?sal .\n"
                + "  	?is <http://ucuenca.edu.ec/ontology#SJR> ?SJR .\n"
                + "  	?is <http://ucuenca.edu.ec/ontology#SNIP> ?SNIP .\n"
                + "  	?pub1uri a foaf:Organization .\n"
                + "  	?pub1uri foaf:name ?pub1 .\n"
                + "  	?pub2uri a foaf:Organization .\n"
                + "  	?pub2uri foaf:name ?pub2 .\n"
                + "  	?is dct:publisher ?pub2uri .\n"
                + "  	?is dct:publisher ?pub1uri .\n"
                + "  	?is <http://purl.org/ontology/bibo/issn> ?issn .\n"
                + "  	?is <http://purl.org/ontology/bibo/issn> ?issn2 .\n"
                + "  	?is <http://purl.org/ontology/bibo/isbn> ?isbn .\n"
                + "  	?is <http://purl.org/ontology/bibo/uri> ?ss .\n"
                + "  	?is <http://purl.org/ontology/bibo/uri> ?hm .\n"
                + "  	?is <http://purl.org/ontology/bibo/uri> ?hm2 .\n"
                + "    }\n"
                + "} where {\n"
                + "  	\n"
                + "  	values ?ty {<http://purl.org/ontology/bibo/" + type + ">}\n"
                + "    values ?is {<" + stringValue + ">} .\n"
                + "	graph <" + ELSEVIER_CONTEXT + "> {\n"
                + "      	?un <prism:url> ?is.\n"
                + "      	?un <dc:title> ?n .\n"
                + "       	optional {\n"
                + "        	?un foaf:img2 ?i .\n"
                + "        }\n"
                + "      	optional {\n"
                + "        	?un <http://purl.org/dc/elements/1.1/subject-area> ?sa .\n"
                + "          	?sa <http://purl.org/dc/elements/1.1/$> ?sal .\n"
                + "        }\n"
                + "        optional {\n"
                + "        	?un <http://purl.org/dc/elements/1.1/SJRList> ?jl .\n"
                + "            ?jl <http://purl.org/dc/elements/1.1/SJR> ?so .\n"
                + "          	?so <http://purl.org/dc/elements/1.1/$> ?SJR .\n"
                + "        }\n"
                + "      	optional {\n"
                + "        	?un <http://purl.org/dc/elements/1.1/SNIPList> ?jl1 .\n"
                + "            ?jl1 <http://purl.org/dc/elements/1.1/SNIP> ?so1 .\n"
                + "          	?so1 <http://purl.org/dc/elements/1.1/$> ?SNIP .\n"
                + "        }\n"
                + "      	optional {\n"
                + "        	?un <dc:publisher> ?pub1 .\n"
                + "          	filter (isLiteral(?pub1)) .\n"
                + "          	bind(encode_for_uri(str(?pub1)) as ?pub1h) .\n"
                + "          	bind (iri(concat('https://redi.cedia.edu.ec/resource/publisher/',?pub1h)) as ?pub1uri) .\n"
                + "        }\n"
                + "      	optional {\n"
                + "        	?un <dc:publisher> ?pub2o .\n"
                + "          	?pub2o <http://purl.org/dc/elements/1.1/$> ?pub2 .\n"
                + "          	bind(encode_for_uri(str(?pub2)) as ?pub2h) .\n"
                + "          	bind (iri(concat('https://redi.cedia.edu.ec/resource/publisher/',?pub2h)) as ?pub2uri) .\n"
                + "        }\n"
                + "      	optional {\n"
                + "        	?un <prism:eIssn> ?issn .\n"
                + "        }\n"
                + "      	optional {\n"
                + "        	?un <prism:issn> ?issn2 .\n"
                + "        }\n"
                + "      	optional {\n"
                + "        	?un <prism:isbn> ?isbn .\n"
                + "        }\n"
                + "      	optional {\n"
                + "        	?un <prism:edition> ?ed .\n"
                + "        }\n"
                + "      	optional {\n"
                + "        	?un <http://purl.org/dc/elements/1.1/link> ?sso .\n"
                + "          	?sso <http://purl.org/dc/elements/1.1/@ref> 'scopus-source'^^<http://www.w3.org/2001/XMLSchema#string> .\n"
                + "          	?sso <http://purl.org/dc/elements/1.1/@href> ?ss .\n"
                + "        }\n"
                + "      	optional {\n"
                + "        	?un <http://purl.org/dc/elements/1.1/link> ?sso1 .\n"
                + "          	?sso1 <http://purl.org/dc/elements/1.1/@ref> 'homepage'^^<http://www.w3.org/2001/XMLSchema#string> .\n"
                + "          	?sso1 <http://purl.org/dc/elements/1.1/@href> ?hm .\n"
                + "        }\n"
                + "      	optional {\n"
                + "        	?un <http://purl.org/dc/elements/1.1/link> ?sso12 .\n"
                + "          	?sso12 <http://purl.org/dc/elements/1.1/@rel> 'homepage'^^<http://www.w3.org/2001/XMLSchema#string> .\n"
                + "          	?sso12 <http://purl.org/dc/elements/1.1/@href> ?hm2 .\n"
                + "        }\n"
                + "	}\n"
                + "}";
        return q2;
    }

    public void mergeElsevierJournals() throws RepositoryException, MalformedQueryException, UpdateExecutionException, QueryEvaluationException {
        String q = getListJournalsElsevierQuery(true, true);
        List<Map<String, Value>> query = query(q);
        log.info("Loading Journals...");
        int i = 0;
        for (Map<String, Value> d : query) {
            String stringValue = d.get("j").stringValue();
            log.info("{} / {} URI = {}", i, query.size(), stringValue);
            String q2 = getUpdateElsevierQuery(stringValue, "Journal");
            update(q2);
            i++;
        }
        q = getListJournalsElsevierQuery(true, false);
        query = query(q);
        log.info("Loading Proceedingss...");
        i = 0;
        for (Map<String, Value> d : query) {
            String stringValue = d.get("j").stringValue();
            log.info("{} / {} URI = {}", i, query.size(), stringValue);
            String q2 = getUpdateElsevierQuery(stringValue, "Proceedings");
            update(q2);
            i++;
        }
        q = getListJournalsElsevierQuery(false, false/*mock*/);
        query = query(q);
        log.info("Loading Books...");
        i = 0;
        for (Map<String, Value> d : query) {
            String stringValue = d.get("j").stringValue();
            log.info("{} / {} URI = {}", i, query.size(), stringValue);
            String q2 = getUpdateElsevierQuery(stringValue, "Book");
            update(q2);
            i++;
        }
    }

    public void linkElsevierJournals() throws RepositoryException, MalformedQueryException, UpdateExecutionException {
        String q = "PREFIX dct: <http://purl.org/dc/terms/>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "insert {\n"
                + "	graph <" + PUB_CONTEXT + "> {\n"
                + "    	?a dct:isPartOf ?c .\n"
                + "    }\n"
                + "} where {\n"
                + "	graph <" + ELSEVIER_CONTEXT + "SameAs> {\n"
                + "  		?a rdfs:seeAlso ?c .\n"
                + "    }\n"
                + "} ";
        update(q);
        q = "PREFIX dct: <http://purl.org/dc/terms/>\n"
                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
                + "insert {\n"
                + "	graph <" + PUB_CONTEXT + "> {\n"
                + "    	?a ?b ?c .\n"
                + "    }\n"
                + "} where {\n"
                + "	graph <" + SCIMAGOJR_CONTEXT + "> {\n"
                + "  		?a ?b ?c .\n"
                + "    }\n"
                + "} ";
        update(q);
    }

    public boolean ask(String q) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        boolean t = false;
        RepositoryConnection connection = this.conn.getConnection();
        connection.begin();
        BooleanQuery prepareBooleanQuery = connection.prepareBooleanQuery(QueryLanguage.SPARQL, q);
        t = prepareBooleanQuery.evaluate();
        connection.commit();
        connection.close();
        return t;
    }

}
