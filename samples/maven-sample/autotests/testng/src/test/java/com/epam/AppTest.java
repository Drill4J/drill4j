/*
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
package com.epam;


import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.*;


/**
 * Simple tests on Petclinic app using testng
 */
public class AppTest 
{
    private final String pathToServer = "http://localhost:8080";
    private final CloseableHttpClient httpclient = HttpClients.createDefault();

    @Test
    public void getOwner4InfoPage() throws IOException {

        HttpGet httpGet = new HttpGet(pathToServer + "/owners/4");
        CloseableHttpResponse response = httpclient.execute(httpGet);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    }

    @Test
    public void getOwner4EditPage() throws IOException {

        HttpGet httpGet = new HttpGet(pathToServer + "/owners/4/edit");
        CloseableHttpResponse response = httpclient.execute(httpGet);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    }

    @Test
    public void getHomePage() throws IOException {
        HttpGet httpGet = new HttpGet(pathToServer + "/");
        CloseableHttpResponse response = httpclient.execute(httpGet);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);
    }

}
