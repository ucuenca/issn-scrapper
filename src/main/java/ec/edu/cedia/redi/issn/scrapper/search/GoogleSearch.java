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
package ec.edu.cedia.redi.issn.scrapper.search;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public class GoogleSearch implements WebSearcher {

    private static final String SEARCH_FORMAT = "https://www.google.com.ec/search?q=%s";
    private final Logger log = LoggerFactory.getLogger(GoogleSearch.class);

    @Override
    public List<String> getUrls(String query, int n) {
        List<String> urls = new ArrayList<>(n);
        try {
            String urlSearch = String.format(SEARCH_FORMAT, URLEncoder.encode(query, "utf-8"));
            Document doc = Jsoup.connect(urlSearch)
                    .userAgent(USER_AGENT).get();
            Elements elements = doc.select(".g .rc .r a");
            for (int i = 0; i < n; i++) {
                String url = elements.get(i).attr("href");
                urls.add(url);
            }
        } catch (IOException ex) {
            log.error("Cannot consume query " + query, ex);
        }
        log.info("Found {}/{} urls for query {}", urls.size(), n, query);
        return urls;
    }

}
