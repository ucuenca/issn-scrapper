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
package ec.edu.cedia.redi.issn.scrapper.api;

import ec.edu.cedia.redi.issn.scrapper.search.WebSearcher;
import ec.edu.cedia.redi.issn.scrapper.search.query.Query;
import ec.edu.cedia.redi.issn.scrapper.search.query.StrictQuery;
import ec.edu.cedia.redi.issn.scrapper.search.query.Value;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLProtocolException;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public class PublicationIssnScrapper implements IssnScrapper {

    private static final Logger log = LoggerFactory.getLogger(PublicationIssnScrapper.class);
    /**
     * When there's a PDF file, get the HTML from Google Cache.
     */
    private static final String GOOGLE_CACHE = "http://webcache.googleusercontent.com/search?q=cache:%s";
    /**
     * Maximum number of results after making a web search.
     */
    private static final int MAX_PAGES = 3;
    private static final String DEFAULT_ISSN_KW = "issn";
    private static final Pattern PATTERN = Pattern.compile("[0-9]{4}\\-?[0-9]{3}[0-9xX]");
    private WebSearcher searcher;

    public PublicationIssnScrapper(WebSearcher web) {
        searcher = web;
    }

    @Override
    public Map<String, List<String>> scrape(String title, String abztract) {
        Map<String, List<String>> results = new ConcurrentHashMap<>();

        Query query;
        if (abztract != null && !"".equals(abztract)) {
            query = new StrictQuery(new Value(title, 25), new Value(abztract, 75), new Value(DEFAULT_ISSN_KW, -1));
        } else {
            query = new StrictQuery(new Value(title, 100), new Value(DEFAULT_ISSN_KW, -1));
        }

        List<String> resultsSearch = searcher.getUrls(query, MAX_PAGES);
        for (String url : resultsSearch) {
            Set<String> issn = new HashSet<>();
            issn.addAll(findIssn(url));
            results.put(url, new ArrayList<>(issn));
        }

        return results;
    }

    @Override
    public Map<String, List<String>> scrape(String title) {
        return scrape(title, null);
    }

    private List<String> findIssn(String url) {
        List<String> issn = new ArrayList<>();
        try {
            Document doc;
            log.debug("Extracting URL: '{}'", url);
            if (url.endsWith("pdf")) {
                doc = Jsoup.connect(String.format(GOOGLE_CACHE, URLEncoder.encode(url, "utf-8")))
                        .userAgent(WebSearcher.USER_AGENT)
                        .get();
            } else {
                doc = Jsoup.connect(url)
                        .timeout(20000)
                        .userAgent(WebSearcher.USER_AGENT)
                        .get();
            }
            Matcher matcher = PATTERN.matcher(doc.text());

            while (matcher.find()) {
                String issnFound = matcher.group();
                if (issnFound.length() == 8) {
                    issnFound = issnFound.substring(0, 4) + "-" + issnFound.substring(4);
                }
                issn.add(issnFound);
            }
        } catch (HttpStatusException | SSLHandshakeException ex) {
            log.warn("Cannot make request, probably google cache does have the url {}", url);
        } catch (UnsupportedMimeTypeException ex) {
            log.warn("Don't understand MIME Type for url {}", url);
        } catch (SocketTimeoutException | SocketException ex) {
            log.warn("{} for {}", ex.getMessage(), url);
        } catch (SSLProtocolException ex) {
            log.warn("Cannot extract ISSNs for url ({}) due to SSL errors.", url);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return issn;
    }
}
