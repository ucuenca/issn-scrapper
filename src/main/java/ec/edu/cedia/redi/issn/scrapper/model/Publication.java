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
package ec.edu.cedia.redi.issn.scrapper.model;

import java.util.List;

/**
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public class Publication {

    private String uri;
    private String title;
    private String abztract;
    private List<Issn> issn;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAbztract() {
        return abztract;
    }

    public void setAbztract(String abztract) {
        this.abztract = abztract;
    }

    public List<Issn> getIssn() {
        return issn;
    }

    public void setIssn(List<Issn> issn) {
        this.issn = issn;
    }

    @Override
    public String toString() {
        String readable = uri + ",";
        if (issn != null && !issn.isEmpty()) {
            for (Issn issn1 : issn) {
                readable += issn1.getUri() + ",";
            }
        }
        return readable;
    }

}
