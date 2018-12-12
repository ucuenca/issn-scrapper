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
package ec.edu.cedia.redi.latindex.execute;

import ec.edu.cedia.redi.latindex.repository.Redi;
import ec.edu.cedia.redi.latindex.repository.RediRepository;
import ec.edu.cedia.redi.latindex.model.Issn;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.openrdf.model.Model;
import org.openrdf.model.impl.TreeModel;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public class AugmentIssnLatindex {

    private static final String PORTAL_ISSN_FORMAT_TURTLE = "https://portal.issn.org/resource/issn/%s?format=turtle";

    private final static Logger log = LoggerFactory.getLogger(AugmentIssnLatindex.class);
    private final Redi redi;

    public static void main(String[] args) throws Exception {
        try (RediRepository r = RediRepository.getInstance()) {
            Redi redi = new Redi(r);
            AugmentIssnLatindex a = new AugmentIssnLatindex(redi);
            a.augmentIssns();
            // a.augmentIssn("1390-6143");
        }
    }

    public AugmentIssnLatindex(Redi redi) {
        this.redi = redi;
    }

    public void augmentIssns() throws RepositoryException, QueryEvaluationException {
        List<Issn> issns = redi.getIssns();
        int size = issns.size();
        for (int i = 0; i < issns.size(); i++) {
            String latIssn = issns.get(i).getIssn();
            log.info("Processing issn ({}) {}/{}", latIssn, i + 1, size);
            augmentIssn(latIssn);
        }
    }

    public void augmentIssn(String issn) throws RepositoryException, QueryEvaluationException {
        Issn latIssn = redi.getIssn(issn);
        Issn roadIssn = redi.getRoadIssn(issn);
        if (roadIssn != null) {
            redi.augmentIssn(latIssn, roadIssn);
        }
    }

    private Model getPortalIssnTriples(String issn) throws RDFParseException {
        InputStream inputStream = null;
        // Read data stream
        try {
            String url = String.format(PORTAL_ISSN_FORMAT_TURTLE, issn);
            java.net.URL documentUrl = new URL(url);
            inputStream = documentUrl.openStream();
            log.info("Requesting {}", url);
            byte[] data = IOUtils.toString(inputStream).replace("dateTime", "string").getBytes();
            inputStream = new ByteArrayInputStream(data);
        } catch (MalformedURLException ex) {
            log.error("The url is incorrect. Issn: " + issn, ex);
        } catch (IOException ex) {
            log.error("", ex);
        }

        // Parse stream to turtle
        try {
            RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
            StatementCollector collector = new StatementCollector();
            rdfParser.setRDFHandler(collector);
            rdfParser.parse(inputStream, "https://redi.cedia.edu.ec/context/portalissn/");
            return new TreeModel(collector.getStatements());
        } catch (IOException | RDFHandlerException ex) {
            log.error("", ex);
        } catch (RDFParseException ex) {
            log.error("Cannot parse input stream to turtle", ex);
        }
        throw new RDFParseException("Cannot get/parse triples for ISSN:" + issn);
    }
}
