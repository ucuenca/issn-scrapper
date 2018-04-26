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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
        Map<String, List<String>> issnsPerPageResults;
        Map<String, List<Issn>> issnsPerPageLatindex = new ConcurrentHashMap<>();
        if (p.getAbztract() == null) {
            issnsPerPageResults = scrapper.scrape(p.getTitle());
        } else {
            issnsPerPageResults = scrapper.scrape(p.getTitle(), p.getAbztract());
        }
        List<Issn> allTrueIssn = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : issnsPerPageResults.entrySet()) {
            List<Issn> issnXPage = new ArrayList<>();
            for (String issnCandidate : entry.getValue()) {
                Issn i = redi.getIssn(issnCandidate);
                if (i != null) {
                    allTrueIssn.add(i);
                    issnXPage.add(i);
                }
            }
            issnsPerPageLatindex.put(entry.getKey(), issnXPage);
        }
        log.info("Found {} ISSN for publication {}", allTrueIssn.size(), p.getUri());
        p.setIssn(allTrueIssn);
        p.setIssnPerPage(issnsPerPageLatindex);
    }

    public static void main(String[] args) throws Exception {
        try (RediRepository r = RediRepository.getInstance()) {
            Redi redi = new Redi(r);
            FindPotentialIssn finder = new FindPotentialIssn(redi, new GoogleSearch());
            List<Publication> publications = redi.getPublications();
            for (Publication p : publications) {
                if (!redi.hasPubPotentialIssn(p)) {
                    finder.findPotentialIssn(p);
                    redi.storePublicationAllPotentialIssn(p);
                }
            }

            for (Publication publication : publications) {
                System.out.println(publication);
            }
        }
    }

}
