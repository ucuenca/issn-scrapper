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

import java.util.Optional;
import org.apache.commons.httpclient.HttpException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.model.Model;

/**
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public class JournalMetricsTest {

    private static JournalMetrics metrics;

    @BeforeClass
    public static void setUpClass() {
        metrics = new JournalMetrics();
    }

    @Test
    public void testJournalInfo() throws HttpException {
        Model rdf = metrics.getJournal("1468-4322");
        Assert.assertEquals(232, rdf.size());
        rdf = metrics.getJournal("1468-4324");
        Assert.assertEquals(0, rdf.size());
    }

    @Test
    public void testJournalImage() throws HttpException {
        Optional<String> img = metrics.getImage("0378-1119");
        Assert.assertTrue(img.get().length() > 0);
        img = metrics.getImage("1468-4322");
        Assert.assertFalse(img.isPresent());
    }

}
