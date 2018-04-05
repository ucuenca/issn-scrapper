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

import ec.edu.cedia.redi.issn.scrappe.latindex.redi.Issn;
import ec.edu.cedia.redi.issn.scrappe.latindex.redi.Publication;
import ec.edu.cedia.redi.issn.scrappe.latindex.redi.Redi;
import ec.edu.cedia.redi.issn.scrappe.latindex.redi.RediRepository;
import ec.edu.cedia.redi.issn.scrapper.api.IssnScrape;
import ec.edu.cedia.redi.issn.scrapper.api.Scrapper;
import ec.edu.cedia.redi.issn.scrapper.search.GoogleSearch;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public class Main {

    public static void main(String[] args) throws Exception {
        IssnScrape is = new Scrapper(new GoogleSearch());

        try (RediRepository r = RediRepository.getInstance()) {
            Redi redi = new Redi(r);
            List<Issn> issn = redi.getIssns();
            List<Publication> publications = redi.getPublications();
            for (Publication publication : publications) {
                for (String i : is.scrape(publication.getTitle())) {
                    List<Issn> trueIssn = selectIssn(issn, i);
                    publication.setIssn(trueIssn);
                }
            }
            
            
            for (Publication publication : publications) {
                System.out.println(publication);
            }
        }
    }

    public static List<Issn> selectIssn(List<Issn> issn, String compareIssn) {
        compareIssn = cleanIssn(compareIssn);
        List<Issn> selected = new ArrayList<>();
        for (Issn issn1 : issn) {
            String latindexIssn = cleanIssn(issn1.getIssn());
            if (latindexIssn.equals(compareIssn)) {
                selected.add(issn1);
            }
        }
        return selected;
    }

    public static String cleanIssn(String issn) {
        return issn.toLowerCase().replace("-", "");
    }
}
