package com.pyin.plugin.client.api.http;

public interface PyinCenterHttpClient {

    String get(String path);

    String post(String path, Object body);
}
