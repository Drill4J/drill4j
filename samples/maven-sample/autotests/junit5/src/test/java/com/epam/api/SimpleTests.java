package com.epam.api;


import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JUnit5 autotest example for petclinic app.
 */
public class SimpleTests
{
    private final String pathToServer = "http://localhost:8080";
    private final CloseableHttpClient httpclient = HttpClients.createDefault();

    @Test
    public void getOwner4InfoPage() throws IOException {

        HttpGet httpGet = new HttpGet(pathToServer + "/owners/4");
        CloseableHttpResponse response = httpclient.execute(httpGet);
        assertEquals(response.getStatusLine().getStatusCode(), 200);
    }


    @Test
    public void getOwner4EditPage() throws IOException {

        HttpGet httpGet = new HttpGet(pathToServer + "/owners/4/edit");
        CloseableHttpResponse response = httpclient.execute(httpGet);
        assertEquals(response.getStatusLine().getStatusCode(), 200);
    }

    @Test
    public void getHomePage() throws IOException {
        HttpGet httpGet = new HttpGet(pathToServer + "/");
        CloseableHttpResponse response = httpclient.execute(httpGet);
        assertEquals(response.getStatusLine().getStatusCode(), 200);
    }

}
