/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.edu.cedia.redi.graphdb.centralgraph;

import com.github.jsonldjava.utils.JsonUtils;
import ec.edu.cedia.redi.latindex.utils.HTTPCaller;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sparql.SPARQLRepository;

/**
 *
 * @author cedia
 */
public class graphdbRepository {

    public static String TMP = "/tmp/";
    public static String GRAPHDB_SCHEMA = "data";
    public static String GRAPHDB_SERVER = "http://201.159.222.25:8180/";
    public static String GRAPHDB_INSTANCE = GRAPHDB_SERVER + "repositories/" + GRAPHDB_SCHEMA + "/statements";
    public static String GRAPHDB_IMPORT = GRAPHDB_SERVER + "rest/data/import/upload/" + GRAPHDB_SCHEMA + "/url";
    public static String REDI_DOWNLOAD = "https://rediclon.cedia.edu.ec/export/download?format=application%2Ftrix&context=";
    public static String REDI_DOWNLOAD_CONTEXTS = "https://rediclon.cedia.edu.ec/context/list?labels=true";
    public static String BASE_CONTEXT = "https://redi.cedia.edu.ec/context/";
    public static String AUTHORS_SA = BASE_CONTEXT + "authorsSameAs";
    public static String COAUTHORS_SA = BASE_CONTEXT + "coauthorsSameAs";
    public static String PUBLICATIONS_SA = BASE_CONTEXT + "publicationsSameAs";
    public static String AUTHORS_SA2 = BASE_CONTEXT + "authorsSameAs2";
    public static String[] REDI_PROVIDERS = {
        BASE_CONTEXT + "provider/ScieloProvider",
        BASE_CONTEXT + "provider/GoogleScholarProvider",
        BASE_CONTEXT + "provider/ScopusProvider",
        BASE_CONTEXT + "provider/DBLPProvider",
        BASE_CONTEXT + "provider/AcademicsKnowledgeProvider",
        BASE_CONTEXT + "provider/SpringerProvider",
        BASE_CONTEXT + "provider/DOAJProvider",
        BASE_CONTEXT + "provider/ORCIDProvider",
        BASE_CONTEXT + "authorsProvider"

    };

    public void cloneREDI() throws Exception {
        Set<String> contexts = getContexts();
        importAllSafeMT(4, contexts);
    }

    public void ImportProviders() throws Exception {
        Set<String> mySet = new HashSet<String>(Arrays.asList(REDI_PROVIDERS));
        importAllSafeMT(4, mySet);
    }

    public Set<String> getContexts() throws Exception {
        String html = getHTML(REDI_DOWNLOAD_CONTEXTS);
        ArrayList<Map<String, Object>> name = (ArrayList<Map<String, Object>>) JsonUtils.fromString(html);
        Set<String> uris = new LinkedHashSet<>();
        for (Map<String, Object> a : name) {
            uris.add(a.get("uri").toString());
        }
        return uris;
    }

    public String run(String... b) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(b);
        pb.directory(new File(TMP));
        Process p = pb.start();
        InputStream in = p.getInputStream();
        InputStream errorStream = p.getErrorStream();
        String toString = IOUtils.toString(in) + IOUtils.toString(errorStream);
        //System.out.println(toString);
        return toString;
    }

    public void prepareCmd(String sc) throws Exception {
        InputStream resourceAsStream = this.getClass().getResourceAsStream("/" + sc + ".sh");
        Files.copy(resourceAsStream, Paths.get(TMP + sc + ".sh"), StandardCopyOption.REPLACE_EXISTING);
        run("chmod", "+x", sc + ".sh");
    }

    public void prepare() throws Exception {
        prepareCmd("upload");
        prepareCmd("clean");
    }

    public void compressAndUpload(String a) throws Exception {
        run("sh", "upload.sh", TMP + a);
    }

    public void clean(String a) throws Exception {
        run("./clean.sh", a);
    }

    public void ImportGraphToFile(String graph, String name) throws MalformedURLException, IOException {
        ImportToFile(REDI_DOWNLOAD + URLEncoder.encode(graph), name);
    }

    public void ImportToFile(String URL, String name) throws MalformedURLException, IOException {
        FileUtils.copyURLToFile(new URL(URL), new File(TMP + name));
    }

    public void removeDuplicatedSameAs(String O, String D) throws RepositoryException, MalformedQueryException, UpdateExecutionException {
        String OT = O + "__T";
        String q1 = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
                + "insert {\n"
                + "    graph <" + OT + "> {\n"
                + "        ?a owl:sameAs ?c .\n"
                + "    }\n"
                + "} where {\n"
                + "    graph <" + O + "> {\n"
                + "        ?a owl:sameAs ?b .\n"
                + "        ?c owl:sameAs ?b .\n"
                + "    }\n"
                + "}";
        runUpdate(q1);
        String OTT = OT + "__T";
        String q2 = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
                + "insert {\n"
                + "    graph <" + OTT + "> {\n"
                + "        ?a owl:sameAs ?c .\n"
                + "    }\n"
                + "} where {\n"
                + "    graph <" + OT + "> {\n"
                + "        ?a owl:sameAs* ?c .\n"
                + "    }\n"
                + "}";
        runUpdate(q2);
        String OTTT = OTT + "__T";
        String q3 = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
                + "insert {\n"
                + "    graph <" + OTTT + "> {\n"
                + "    	?p owl:sameAs ?q .\n"
                + "	} \n"
                + "} where {\n"
                + "    {\n"
                + "        select ?h (min(?a) as ?p) {\n"
                + "            {\n"
                + "                select ?a (md5(group_concat(?c)) as ?h) {\n"
                + "                    {\n"
                + "                        select ?a ?c {\n"
                + "                            graph <" + OTT + "> {\n"
                + "                                ?a owl:sameAs ?c .\n"
                + "                            } \n"
                + "                        } order by ?a ?c\n"
                + "                    }\n"
                + "                } group by ?a\n"
                + "            }\n"
                + "        } group by ?h\n"
                + "    }\n"
                + "    graph <" + OTT + "> {\n"
                + "    	?p owl:sameAs ?q .\n"
                + "	} \n"
                + "}";
        runUpdate(q3);
        replaceSameAsSubject(O, D, OTTT);
        deleteGraph(OT);
        deleteGraph(OTT);
        deleteGraph(OTTT);

    }

    public void removeDuplicatedPrior(String G1, String G2, String G, String D) throws RepositoryException, MalformedQueryException, UpdateExecutionException {
        String q = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\n"
                + "insert {\n"
                + "    graph <&D&> {\n"
                + "        ?q2 owl:sameAs ?p2 .\n"
                + "    }\n"
                + "    graph <&I&> {\n"
                + "        ?q1 owl:sameAs ?p2 .\n"
                + "    }\n"
                + "} \n"
                + "where  {\n"
                + "    {   \n"
                + "        select ?c {\n"
                + "            graph <" + G + "> {\n"
                + "                ?a owl:sameAs ?c .\n"
                + "            }\n"
                + "        } group by ?c having (count (distinct ?a ) > 1 ) \n"
                + "    }\n"
                + "    graph <" + G + "> {\n"
                + "        ?q1 owl:sameAs ?c .\n"
                + "        ?q2 owl:sameAs ?c .\n"
                + "    }\n"
                + "    graph <" + G1 + "> {\n"
                + "        ?q1 owl:sameAs [] .\n"
                + "    }\n"
                + "    graph <" + G2 + "> {\n"
                + "        ?q2 owl:sameAs ?p2 .\n"
                + "    }\n"
                + "}";
        transformGraph(G, D, q);
    }

    public void harvestRawData(String S, String D, int up) throws RepositoryException, MalformedQueryException, UpdateExecutionException {
        for (int i = 1; i <= up; i++) {
            harvestSameAs(S, D, i);
        }
    }

    public void harvestSameAs(String S, String D, int level) throws RepositoryException, MalformedQueryException, UpdateExecutionException {
        String providers = "";
        for (String p : REDI_PROVIDERS) {
            providers = " <" + p + "> ";

            String qp = "";
            String last = "";
            for (int i = 0; i < level; i++) {
                String j = i == 0 ? "" : "" + (i - 1);
                last = "    ?c" + j + " ?p" + i + " ?c" + i + " .\n";
                qp += last;
            }

            String q = "insert {\n"
                    + "  graph <" + D + "> {\n"
                    + last
                    + "  }\n"
                    + "}\n"
                    + "where {\n"
                    + "  graph <" + S + "> {\n"
                    + "    ?a <http://www.w3.org/2002/07/owl#sameAs> ?c .\n"
                    + "  }\n"
                    + "  values ?g { " + providers + " } .\n"
                    + "  graph ?g {\n"
                    + qp
                    + "  }\n"
                    + "}";
            runUpdate(q);
        }
    }

    public void replaceSameAsObjectExcept(String O, String D, String S, String... E) throws RepositoryException, MalformedQueryException, UpdateExecutionException {
        String qe = "";
        if (E.length != 0) {
            qe = "filter ( ";
            for (int i = 0; i < E.length; i++) {
                qe += " ?p != <" + E[i] + "> ";
                if (i != E.length - 1) {
                    qe += " && ";
                }
            }
            qe += " ) .";
        }
        String q = "insert {\n"
                + "        graph <&D&> {\n"
                + "                ?v ?p ?c .\n"
                + "        }\n"
                + "        graph <&I&> {\n"
                + "                ?v ?p ?a .\n"
                + "        }\n"
                + "}\n"
                + "where {\n"
                + "        graph <" + S + "> {\n"
                + "                ?a <http://www.w3.org/2002/07/owl#sameAs> ?c .\n"
                + "        }\n"
                + "        graph <" + O + "> {\n"
                + "                ?v ?p ?c .\n"
                + "                " + qe + "\n"
                + "        }\n"
                + "}";
        transformGraph(O, D, q);
    }

    public void replaceSameAsSubject(String O, String D, String S) throws RepositoryException, MalformedQueryException, UpdateExecutionException {
        String q = "insert {\n"
                + "        graph <&D&> {\n"
                + "                ?c ?p ?v .\n"
                + "        }\n"
                + "        graph <&I&> {\n"
                + "                ?a ?p ?v .\n"
                + "        }\n"
                + "}\n"
                + "where {\n"
                + "        graph <" + S + "> {\n"
                + "                ?a <http://www.w3.org/2002/07/owl#sameAs> ?c .\n"
                + "        }\n"
                + "        graph <" + O + "> {\n"
                + "                ?c ?p ?v .\n"
                + "        }\n"
                + "}";
        transformGraph(O, D, q);
    }

    public void transformGraph(String O, String D, String Q) throws RepositoryException, MalformedQueryException, UpdateExecutionException {
        String OI = O + "___I";
        String OD = O + "___D";
        String UQ = Q.replaceAll("&I&", OI).replaceAll("&D&", OD);
        runUpdate(UQ);
        copyGraphFilter(O, OD, D);
        copyGraph(OI, D);
        deleteGraph(OI);
        deleteGraph(OD);
    }

    public void importAll() throws UnsupportedEncodingException {
        for (String gu : graphdbRepository.REDI_PROVIDERS) {
            importGraph(gu);
        }
    }

    public void importAllSafe() throws UnsupportedEncodingException, Exception {
        for (String gu : graphdbRepository.REDI_PROVIDERS) {
            importGraphSafe(gu);
        }
    }

    public void importAllSafeMT(int threads, Set<String> contexts) throws UnsupportedEncodingException, Exception {
        BoundedExecutor threadPool = BoundedExecutor.getThreadPool(threads);

        for (String gu : contexts) {
            final String q = gu;
            threadPool.submitTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        importGraphSafe(q);
                    } catch (Exception ex) {
                        Logger.getLogger(graphdbRepository.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });

        }
        threadPool.end();
    }

    public void importGraph(String gra) throws UnsupportedEncodingException {
        importURL(REDI_DOWNLOAD + URLEncoder.encode(gra));
    }

    public void importGraphSafe(String gra) throws IOException, Exception {
        String toString = UUID.randomUUID().toString();
        System.out.println("DOWNLOAD " + gra);
        ImportToFile(REDI_DOWNLOAD + URLEncoder.encode(gra), toString + ".trix");
        System.out.println("CLEAN " + gra);
        clean(toString + ".trix");
        System.out.println("UPLOAD " + gra);
        compressAndUpload(toString + ".trix");
        System.out.println("IMPORT " + gra);
        importURL("https://rediclon.cedia.edu.ec/fs/UploadDownloadFileServlet?fileName=" + toString + ".trix.gz");
    }

    public void importURL(String URL) throws UnsupportedEncodingException {
        String q = "{\"name\":\"" + URL + "\",\"status\":\"DONE\",\"message\":\"Imported successfully in 1s.\",\"context\":\"\",\"replaceGraphs\":[],\"baseURI\":\"https://redi.cedia.edu.ec/context/\",\"forceSerial\":false,\"type\":\"url\",\"format\":\"\",\"data\":\"" + URL + "\",\"timestamp\":1531936348485,\"parserSettings\":{\"preserveBNodeIds\":false,\"failOnUnknownDataTypes\":false,\"verifyDataTypeValues\":false,\"normalizeDataTypeValues\":false,\"failOnUnknownLanguageTags\":false,\"verifyLanguageTags\":false,\"normalizeLanguageTags\":false,\"verifyURISyntax\":false,\"verifyRelativeURIs\":false,\"stopOnError\":false}}";
        runPostJSONEntity(q);
    }

    public void deleteAll() {
        NameValuePair[] params = {new NameValuePair("update", "CLEAR ALL")};
        runPost(params);
    }

    public void deleteGraph(String uri) throws RepositoryException, MalformedQueryException, UpdateExecutionException {
        runUpdateRepository("CLEAR GRAPH <" + uri + ">");
    }

    public void copyGraph(String org, String des) throws RepositoryException, MalformedQueryException, UpdateExecutionException {
        String q = "insert {\n"
                + "    graph <" + des + "> {\n"
                + "        ?a ?b ?c .\n"
                + "    }\n"
                + "} where {\n"
                + "    graph <" + org + "> {\n"
                + "        ?a ?b ?c .\n"
                + "    }\n"
                + "}";
        runUpdate(q);
    }

    public void copyGraphFilter(String org, String fil, String des) throws RepositoryException, MalformedQueryException, UpdateExecutionException {
        String q = "insert {\n"
                + "    graph <" + des + "> {\n"
                + "        ?a ?b ?c .\n"
                + "    }\n"
                + "} where {\n"
                + "    graph <" + org + "> {\n"
                + "        ?a ?b ?c .\n"
                + "        filter not exists {\n"
                + "             graph <" + fil + "> {\n"
                + "                 ?a ?b ?c .\n"
                + "             }\n"
                + "        }\n"
                + "    }\n"
                + "}";
        runUpdate(q);
    }

    public void runUpdate(String query) throws RepositoryException, MalformedQueryException, UpdateExecutionException {
        runUpdateRepository(query);
    }

    public void runUpdateRepository(String query) throws RepositoryException, MalformedQueryException, UpdateExecutionException {
        System.out.println("update="+URLEncoder.encode(query)+"&infer=true&sameAs=true");
        if (true) {
            return;
        }
        SPARQLRepository sp = new SPARQLRepository("http://201.159.222.25:8180/repositories/data", GRAPHDB_INSTANCE);
        sp.initialize();
        RepositoryConnection connection = sp.getConnection();
        connection.begin();
        connection.prepareUpdate(QueryLanguage.SPARQL, query).execute();
        connection.commit();
        connection.close();
        sp.shutDown();
    }

    public void runPost(NameValuePair[] params) {
        PostMethod postMethod = new PostMethod(GRAPHDB_INSTANCE);
        postMethod.setRequestBody(params);
        int get = HTTPCaller.get(postMethod);
        assert get == 200;
    }

    public void runPostJSONEntity(String params) throws UnsupportedEncodingException {
        PostMethod postMethod = new PostMethod(GRAPHDB_IMPORT);
        postMethod.setRequestEntity(new StringRequestEntity(params, "application/json", Charset.defaultCharset().toString()));
        int get = HTTPCaller.get(postMethod);
        assert get == 200;
    }

    public static String getHTML(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        return result.toString();
    }
}
