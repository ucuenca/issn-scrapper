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

import ec.edu.cedia.redi.issn.scrapper.search.GoogleSearch;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public class ScrapperTest {

    /**
     * Test of scrape method, of class Scrapper.
     */
    @Test
    public void testScrape_String_String() {
        String title = "Detecting similar areas of knowledge using semantic and data mining technologies";
        IssnScrape scrapper = new Scrapper(new GoogleSearch());
        Assert.assertEquals(scrapper.scrape(title, "").size(), 9);
    }

    /**
     * Test of scrape method, of class Scrapper.
     */
    @Test
    public void testScrape_String() {
        String title = "Detecting similar areas of knowledge using semantic and data mining technologies";
        IssnScrape scrapper = new Scrapper(new GoogleSearch());
        Assert.assertEquals(scrapper.scrape(title).size(), 9);
    }

}
