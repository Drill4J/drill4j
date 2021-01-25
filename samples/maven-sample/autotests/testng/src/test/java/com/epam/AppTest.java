package com.epam;


import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.junit.*;

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
