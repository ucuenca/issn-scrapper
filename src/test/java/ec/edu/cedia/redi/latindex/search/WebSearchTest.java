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
package ec.edu.cedia.redi.latindex.search;

import ec.edu.cedia.redi.latindex.api.Query;
import ec.edu.cedia.redi.latindex.api.WebSearcher;
import ec.edu.cedia.redi.latindex.search.query.StrictQuery;
import ec.edu.cedia.redi.latindex.search.query.Value;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public class WebSearchTest {

    public WebSearchTest() {
    }
    private static Query query;

    @BeforeClass
    public static void setUpClass() {
        query = new StrictQuery(new Value("it works", 100));
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Test
    public void testGoogleSearch() {
        WebSearcher google = new GoogleSearch();
        assertEquals(5, google.getUrls(query, 6).size());
    }

    @Test
    public void testBingSearch() {
        WebSearcher bing = new BingSearch();
        assertEquals(10, bing.getUrls(query, 10).size());
    }

    @Test
    public void testBuildQuery() {
        Query q = new StrictQuery(new Value("Identificación automática de artículos indexados en Latindex", 25),
                new Value("Identifying researchers with accepted articles in relevant indexed repositories has become   \n"
                        + "increasingly important in higher education, especially in Ecuador, where Latindex is one of \n"
                        + "the most popular repositories. However, there is no automatic method to identify if an article \n"
                        + "has been indexed in that repository and currently higher-education institutes (HEI) in \n"
                        + "Ecuador have to manually recollect data about their indexed publications, providing control \n"
                        + "entities with information difficult to verify. For this reason, in this paper we present an \n"
                        + "approach to allow HEI and educational authorities to find publications that are indexed in \n"
                        + "Latindex using a set of strategies, with the aim of providing a process to identify indexed \n"
                        + "publications. Additionally, we implemented this approach as a prototype and evaluated it \n"
                        + "with a sample of publications of Ecuadorian researchers, demonstrating that the", 75),
                new Value("issn", -1));
        assertEquals(220, q.buildQueryWords().length());
        assertEquals(225, q.buildQueryCharacters().length());
    }

}
