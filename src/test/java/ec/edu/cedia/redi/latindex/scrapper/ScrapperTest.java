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
package ec.edu.cedia.redi.latindex.scrapper;

import ec.edu.cedia.redi.latindex.api.IssnScrapper;
import ec.edu.cedia.redi.latindex.api.IssnScrapper;
import ec.edu.cedia.redi.latindex.scrapper.PublicationIssnScrapper;
import ec.edu.cedia.redi.latindex.search.GoogleSearch;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public class ScrapperTest {

    IssnScrapper scrapper;

    @Before
    public void init() {
        scrapper = new PublicationIssnScrapper(new GoogleSearch());

    }

    /**
     * Test of scrape method with only title.
     */
    @Test
    public void testScrapeTitleResults() {
        String title = "Detecting similar areas of knowledge using semantic and data mining technologies";
        assertEquals("Size does not match", scrapper.scrape(title).size(), 3);
    }

    /**
     * Test of scrape method with only title when there are not results..
     */
    @Test
    public void testScrapeTitleNoResults() {
        String title = "Plataforma para la b�squeda por contenido visual y sem�ntico de im�genes m�dicas";
        assertEquals("Size does not match", scrapper.scrape(title).size(), 0);
    }

    /**
     * Test of scrape method with abstract and title.
     */
    @Test
    public void testScrape_String() {
        String title = "Detecting similar areas of knowledge using semantic and data mining technologies";
        String abztract = "Searching for scientific publications online is an essential task for researchers \n"
                + "working on a certain topic. However, the extremely large amount of scientific publications \n"
                + "found in the web turns the process of finding a publication into a very difficult task whereas, \n"
                + "locating peers interested in collaborating on a specific topic or reviewing literature is even \n"
                + "more challenging. In this paper, we propose a novel architecture to join multiple \n"
                + "bibliographic sources, with the aim of identifying common research areas and potential \n"
                + "collaboration networks, through a combination of ontologies, vocabularies, and Linked Data \n"
                + "technologies for enriching a base data model. ";
        assertEquals("Size does not match", scrapper.scrape(title, abztract).size(), 2);
    }

}
