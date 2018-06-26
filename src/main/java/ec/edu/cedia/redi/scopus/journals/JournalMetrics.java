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
package ec.edu.cedia.redi.scopus.journals;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import ec.edu.cedia.redi.latindex.utils.HTTPCaller;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.openrdf.model.Model;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.Rio;
import org.openrdf.rio.UnsupportedRDFormatException;
import org.slf4j.LoggerFactory;

/**
 * Retrieve Scopus' new CiteScore metrics, as well as Source Normalized Impact
 * per Paper (SNIP) and SCImago Journal Rank (SJR) metrics and other percentages
 * of certain metadata
 *
 * @see https://dev.elsevier.com/journal_metrics.html
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public class JournalMetrics {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(JournalMetrics.class);

    private final String TITLE = "https://api.elsevier.com/content/serial/title";
    private final String ISSN = "https://api.elsevier.com/content/serial/title/issn/%s";
    private final String ISBN = "https://api.elsevier.com/content/nonserial/title/isbn/%s";
    private final String API_KEY = "a3b64e9d82a8f7b14967b9b9ce8d513d";

    /**
     * Get a {@link Model} from the Journal Metrics API for a given ISSN.
     *
     * @param issn
     * @return
     * @throws org.apache.commons.httpclient.HttpException
     * @see https://dev.elsevier.com/documentation/SerialTitleAPI.wadl
     */
    public Model getJournal(String issn) throws HttpException {
        HttpMethod get = new GetMethod(String.format(ISSN, issn));
        NameValuePair[] params = {
            new NameValuePair("view", "ENHANCED"), //
        };
        get.setQueryString(params);
        get.setRequestHeader("Accept", "application/json");
        get.setRequestHeader("X-ELS-APIKey", API_KEY);

        int status = HTTPCaller.get(get);

        switch (status) {
            case 200:
                try {
                    return getModel(get.getResponseBodyAsStream());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } finally {
                    get.releaseConnection();
                }
            case 404:
                return new LinkedHashModel();
            default:
                throw new HttpException(String.format("Cannot extract "
                        + "journal information for url \n%s. "
                        + "\nStatus code: %s "
                        + "\nISSN:%s", get.getPath(), status, issn));
        }
    }

    /**
     * Get a {@link Model} from the Journal Metrics API for a given ISBN.
     *
     * @param isbn
     * @throws org.apache.commons.httpclient.HttpException
     * @see https://dev.elsevier.com/documentation/NonSerialTitleAPI.wadl
     * @return
     */
    public Model getBook(String isbn) throws HttpException {
        HttpMethod get = new GetMethod(String.format(ISBN, isbn));
        get.setRequestHeader("X-ELS-APIKey", API_KEY);

        int status = HTTPCaller.get(get);
        switch (status) {
            case 200:
                try {
                    return getModel(get.getResponseBodyAsStream());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } finally {
                    get.releaseConnection();
                }
            case 404:
                return new LinkedHashModel();
            default:
                throw new HttpException(String.format("Cannot extract "
                        + "book information for url \n%s. "
                        + "\nStatus code: %s "
                        + "\nISSN:%s", get.getPath(), status, isbn));
        }
    }

    /**
     * Return the URL, if present, for the image cover of a book.
     *
     * @param issn
     * @return
     * @throws HttpException
     */
    public Optional<String> getJournalImage(String issn) throws HttpException {
        return getImage(String.format(ISSN, issn));
    }

    /**
     * Return the URL, if present, for the image cover of a book.
     *
     * @param isbn
     * @return
     * @throws HttpException
     */
    public Optional<String> getBookImage(String isbn) throws HttpException {
        return getImage(String.format(ISBN, isbn));
    }

    private Model getModel(InputStream in) {
        try {
            Object jsonObject = JsonUtils.fromInputStream(in);
            Map context = new HashMap();
            context.put("@vocab", "http://purl.org/dc/elements/1.1/");
            ((LinkedHashMap) jsonObject).put("@context", context);
            JsonLdOptions options = new JsonLdOptions();
            Object compact = JsonLdProcessor.compact(jsonObject, context, options);
            return Rio.parse(new ByteArrayInputStream(
                    JsonUtils.toString(compact).
                    getBytes(StandardCharsets.UTF_8)), "http://example.com/", RDFFormat.JSONLD);
        } catch (IOException | RDFParseException | UnsupportedRDFormatException | JsonLdError ex) {
            throw new RuntimeException(ex);
        }
    }

    private Optional<String> getImage(String url) throws HttpException {
        HttpMethod get = new GetMethod(url);
        NameValuePair[] params = {
            new NameValuePair("view", "COVERIMAGE"), //
        };
        get.setQueryString(params);
        get.setRequestHeader("Accept", "image/gif");

        int status = HTTPCaller.get(get);
        switch (status) {
            case 200:
                return Optional.of(get.getURI().toString());
            case 404:
                return Optional.empty();
            default:
                throw new HttpException(String.format("Cannot extract "
                        + "image \n"
                        + "\nStatus code: %s "
                        + "\nURL:%s", status, url));
        }
    }
}
