package com.epam.drill;


import com.epam.drill.storage.RequestContainer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestParser {

    private String _requestLine;
    private Map<String, String> requestHeaders;
    private Map<String, String> cookies;
    private StringBuffer messageBody;

    public Map<String, String> getCookies() {
        return cookies;
    }

    public HttpRequestParser() {
        requestHeaders = new HashMap<String, String>();
        cookies = new HashMap<String, String>();
        messageBody = new StringBuffer();
    }

    public void parseRequest(String request) throws IOException, RuntimeException {
        BufferedReader reader = new BufferedReader(new StringReader(request));

        String requestLine = reader.readLine();
        if (requestLine == null) {
            return;
        }
        setRequestLine(requestLine); // Request-Line ; Section 5.1

        String header = reader.readLine();

        if (header != null) {
            while (header.length() > 0) {
                appendHeaderParameter(header);
                String header1 = reader.readLine();
                if (header1 == null)
                    break;
                header = header1;

            }

            String bodyLine = reader.readLine();
            while (bodyLine != null) {
                appendMessageBody(bodyLine);
                bodyLine = reader.readLine();
            }
        }

        String cookie = getHeaderParam("Cookie");
        if (cookie != null) {
            String[] split = cookie.split("; ");
            for (String rawCookie : split) {
                String[] cook = rawCookie.split("=");
                if (cook.length == 2)
                    cookies.put(cook[0], cook[1]);
            }
        }
    }

    public String getRequestLine() {
        return _requestLine;
    }

    private void setRequestLine(String requestLine) throws RuntimeException {
        if (requestLine == null || requestLine.length() == 0) {
            throw new RuntimeException("Invalid Request-Line: " + requestLine);
        }
        _requestLine = requestLine;
    }

    private void appendHeaderParameter(String header) throws RuntimeException {
        int idx = header.indexOf(":");
        if (idx == -1) {
            throw new RuntimeException("Invalid Header Parameter: " + header);
        }
        requestHeaders.put(header.substring(0, idx), header.substring(idx + 1, header.length()));
    }

    public String getMessageBody() {
        return messageBody.toString();
    }

    private void appendMessageBody(String bodyLine) {
        messageBody.append(bodyLine).append("\r\n");
    }

    public String getHeaderParam(String headerName) {
        return requestHeaders.get(headerName);
    }

    public static void hi(Object $3) {
        if ($3 instanceof ByteBuffer) {
            ByteBuffer $31 = (ByteBuffer) $3;
            $31.flip();
            byte[] bytes = new byte[$31.remaining()];
            $31.get(bytes);
            String x = new String(bytes);
            try {
                HttpRequestParser rp = new HttpRequestParser();
                rp.parseRequest(x.trim());

                RequestContainer.setResource(rp);
                System.out.println("[" + Thread.currentThread().getName() + "]" + "we set httRequest to storage");
                System.out.println(rp.cookies.get("DrillSessionId"));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}