/**
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam

import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import spock.lang.Specification

class SimpleTest extends Specification {

    private final String pathToServer = "http://localhost:8080";
    private final CloseableHttpClient httpclient = HttpClients.createDefault();

    def 'getOwner4InfoPage'() {
        when:
        "Request to petclinc $pathToServer/owners/4"
        HttpGet httpGet = new HttpGet("$pathToServer/owners/4");
        CloseableHttpResponse response = httpclient.execute(httpGet);
        then:
        response.getStatusLine().getStatusCode() == 200
    }

    def 'getOwner4EditPage'() {
        when:
        "Request to petclinc $pathToServer/owners/4/edit"
        HttpGet httpGet = new HttpGet("$pathToServer/owners/4");
        CloseableHttpResponse response = httpclient.execute(httpGet);
        then:
        response.getStatusLine().getStatusCode() == 200
    }
}

