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
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public class PublicationIssnCollector {

    private static final Logger log = LoggerFactory.getLogger(PublicationIssnCollector.class);
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

    public PublicationIssnCollector(WebSearcher searcher) {
        this.searcher = searcher;
    }

    public List<String> collect(String title, String abztract) {
        Set<String> issn = new HashSet<>();
        String query;
        if (abztract != null && !"".equals(abztract)) {
            query = String.format("\"%s\" \"%s\" \"%s\"", title, abztract, DEFAULT_ISSN_KW);
        } else {
            query = String.format("\"%s\" \"%s\"", title, DEFAULT_ISSN_KW);
        }

        List<String> resultsSearch = searcher.getUrls(query, MAX_PAGES);
        for (String url : resultsSearch) {
            issn.addAll(findIssn(url));
        }

        return new ArrayList<>(issn);
    }

    public List<String> collect(String title) {
        return collect(title, null);
    }

    private List<String> findIssn(String url) {
        List<String> issn = new ArrayList<>();
        try {
            Document doc;
            if (url.endsWith("pdf")) {
                doc = Jsoup.connect(String.format(GOOGLE_CACHE, URLEncoder.encode(url, "utf-8")))
                        .userAgent(WebSearcher.USER_AGENT)
                        .get();
            } else {
                doc = Jsoup.connect(url)
                        .userAgent(WebSearcher.USER_AGENT)
                        .get();
            }
            Matcher matcher = PATTERN.matcher(doc.body().text());

            while (matcher.find()) {
                issn.add(matcher.group());
            }
        } catch (IOException ex) {
            log.error("Cannot GET request for url" + url, ex);
        }
        return issn;
    }

}
