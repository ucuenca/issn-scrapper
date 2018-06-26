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
package ec.edu.cedia.redi.latindex.utils;

import java.io.IOException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 *
 * @author Xavier Sumba <xavier.sumba93@ucuenca.ec>
 */
public final class HTTPCaller {

    private static HttpClient client = new HttpClient();

    public static int get(String url) {
        HttpMethod method = new GetMethod(url);
        try {
            return client.executeMethod(method);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static int get(HttpMethod method) {
        try {
            return client.executeMethod(method);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
