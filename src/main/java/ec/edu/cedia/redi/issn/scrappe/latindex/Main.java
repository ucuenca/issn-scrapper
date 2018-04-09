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
package ec.edu.cedia.redi.issn.scrappe.latindex;

import ec.edu.cedia.redi.issn.scrappe.latindex.redi.Publication;
import ec.edu.cedia.redi.issn.scrappe.latindex.redi.Redi;
import ec.edu.cedia.redi.issn.scrappe.latindex.redi.RediRepository;
import ec.edu.cedia.redi.issn.scrapper.search.GoogleSearch;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        try (RediRepository r = RediRepository.getInstance()) {
            Redi redi = new Redi(r);
            FindPotentialIssn finder = new FindPotentialIssn(redi, new GoogleSearch());
            List<Publication> publications = redi.getPublications();
            for (Publication p : publications) {
                if (!redi.hasPubPotentialIssn(p)) {
                    finder.findPotentialIssn(p);
                    redi.storePublication(p);
                }
            }

            for (Publication publication : publications) {
                System.out.println(publication);
            }
        }
    }
}
