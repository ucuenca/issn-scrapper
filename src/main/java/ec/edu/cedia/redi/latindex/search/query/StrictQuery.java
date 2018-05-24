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
package ec.edu.cedia.redi.latindex.search.query;

import ec.edu.cedia.redi.latindex.api.Query;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public class StrictQuery extends Query {

    private List<Value> parameters = new ArrayList<>();

    public StrictQuery(Value v1, Value... vn) {
        parameters.add(v1);
        parameters.addAll(Arrays.asList(vn));

        int freq = parameters.stream()
                .filter(v -> v.getFrequency() > 0)
                .map(v -> v.getFrequency())
                .reduce((a, b) -> a + b).get();
        Preconditions.checkState(freq == 100, "Frequencies must add 100%.");
    }

    @Override
    protected List<Value> getParameters() {
        return parameters;
    }

    @Override
    protected String template() {
        return "\"%s\" ";
    }

    @Override
    protected int extraCharactersInTemplate() {
        return 3;
    }

}
