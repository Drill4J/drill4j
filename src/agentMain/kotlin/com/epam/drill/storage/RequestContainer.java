package com.epam.drill.storage;

import com.epam.drill.HttpRequestParser;

public class RequestContainer {

    private InheritableThreadLocal<HttpRequestParser> requestContainer = new InheritableThreadLocal<HttpRequestParser>();
    private static RequestContainer ourInstance = new RequestContainer();

    public static RequestContainer getInstance() {
        return ourInstance;
    }

    private RequestContainer() {
    }

    public static void setResource(HttpRequestParser res) {
        getInstance().requestContainer.set(res);
    }

    public static HttpRequestParser resource() {
        return getInstance().requestContainer.get();
    }
}
