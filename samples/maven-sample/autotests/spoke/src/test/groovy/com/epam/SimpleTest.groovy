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

