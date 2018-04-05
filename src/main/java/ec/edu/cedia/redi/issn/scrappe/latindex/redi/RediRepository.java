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
package ec.edu.cedia.redi.issn.scrappe.latindex.redi;

import java.util.HashMap;
import java.util.Map;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sparql.SPARQLRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public class RediRepository implements AutoCloseable{
    public final static String ENDPOINT_REDI = "https://rediclon.cedia.edu.ec/sparql";
    private final static String QUERY_ENDPOINT_REDI = ENDPOINT_REDI + "/select";
    private final static String UPDATE_ENDPOINT_REDI = ENDPOINT_REDI + "/update";
    public final static String DEFAULT_CONTEXT = "https://redi.cedia.edu.ec/context/redi";
    private static final Logger log = LoggerFactory.getLogger(RediRepository.class);
    private static final Map<String, String> headers = new HashMap<>();
    private SPARQLRepository repository;

    static {
        headers.put("Accept", "application/ld+json");
    }
    
    private static RediRepository instance;

    private RediRepository() throws RepositoryException {
        repository = new SPARQLRepository(QUERY_ENDPOINT_REDI, UPDATE_ENDPOINT_REDI);
        repository.initialize();
        log.debug("Initializing a repository REDI endpoint.");
    }

    public static synchronized RediRepository getInstance() throws RepositoryException {
        if (instance == null) {
            return new RediRepository();
        }
        return instance;
    }

    /**
     * Get the connection for the {@link Repository} to the REDI repository.
     *
     * @return @throws RepositoryException
     */
    public RepositoryConnection getConnection() throws RepositoryException {
        return repository.getConnection();
    }

    @Override
    public void close() throws Exception {
        if (repository != null) {
            repository.shutDown();
        }
    }
}
