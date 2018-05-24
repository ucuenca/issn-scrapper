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
package ec.edu.cedia.redi.latindex.api;

import java.util.List;
import java.util.Map;

/**
 * Find potential ISSN numbers in a web search.
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public interface IssnScrapper {

    /**
     * Given a title and a abstract find potential ISSN numbers in a web search.
     *
     * @param title
     * @param abztract
     * @return
     */
    public Map<String, List<String>> scrape(String title, String abztract);

    /**
     * Given a title, find potential ISSN numbers in a web search.
     *
     * @param title
     * @return
     */
    public Map<String, List<String>> scrape(String title);
}
