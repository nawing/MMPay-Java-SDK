package com.mmpay.sdk.model;

public class SDKOptions {
    public String appId;
    public String publishableKey;
    public String secretKey;
    public String apiBaseUrl;

    public SDKOptions(String appId, String publishableKey, String secretKey, String apiBaseUrl) {
        this.appId = appId;
        this.publishableKey = publishableKey;
        this.secretKey = secretKey;
        this.apiBaseUrl = apiBaseUrl;
    }
}