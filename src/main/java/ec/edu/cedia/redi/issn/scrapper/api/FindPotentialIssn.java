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
 * distributed under the License scrapper distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ec.edu.cedia.redi.issn.scrapper.api;

import ec.edu.cedia.redi.issn.scrappe.redi.repository.Redi;
import ec.edu.cedia.redi.issn.scrappe.redi.repository.RediRepository;
import ec.edu.cedia.redi.issn.scrapper.model.Issn;
import ec.edu.cedia.redi.issn.scrapper.model.Publication;
import ec.edu.cedia.redi.issn.scrapper.search.GoogleSearch;
import ec.edu.cedia.redi.issn.scrapper.search.WebSearcher;
import java.util.ArrayList;
import java.util.List;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public class FindPotentialIssn {

    private static final Logger log = LoggerFactory.getLogger(FindPotentialIssn.class);
    private final IssnScrapper scrapper;
    private final Redi redi;

    public FindPotentialIssn(Redi redi, WebSearcher searcher) {
        this.redi = redi;
        scrapper = new PublicationIssnScrapper(searcher);
    }

    /**
     * Finds potential ISSNs for a publication and updates its current ISSN
     * list.
     *
     * @param p
     * @throws RepositoryException
     */
    public void findPotentialIssn(Publication p) throws RepositoryException {
        List<String> issnCandidates;
        if (p.getAbztract() == null) {
            issnCandidates = scrapper.scrape(p.getTitle());
        } else {
            issnCandidates = scrapper.scrape(p.getTitle(), p.getAbztract());
        }
        List<Issn> trueIssn = new ArrayList<>();
        for (String issnCandidate : issnCandidates) {
            Issn i = redi.getIssn(issnCandidate);
            if (i != null) {
                trueIssn.add(i);
            }
        }
        log.info("Found {}/{} ISSN for publication {}", trueIssn.size(), issnCandidates.size(), p.getUri());
        p.setIssn(trueIssn);
    }

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
