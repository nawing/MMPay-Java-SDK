package com.mmpay.sdk;

import com.mmpay.sdk.model.Item;
import com.mmpay.sdk.model.PaymentRequest;
import com.mmpay.sdk.model.PayGetRequest;
import com.mmpay.sdk.model.SDKOptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

// --- Main SDK Class ---
public class MMPaySDK {

    private final String appId;
    private final String publishableKey;
    private final String secretKey;
    private final String apiBaseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private String btoken;

    public MMPaySDK(SDKOptions options) {
        this.appId = options.appId;
        this.publishableKey = options.publishableKey;
        this.secretKey = options.secretKey;
        this.apiBaseUrl = options.apiBaseUrl.replaceAll("/$", ""); // Remove trailing slash
        
        this.httpClient = HttpClient.newHttpClient();
        
        // Configure Jackson to mimic JS JSON.stringify (Compact, ignore nulls)
        this.mapper = new ObjectMapper();
        this.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    // --- Core Helpers ---

    private String getNonce() {
        return String.valueOf(Instant.now().toEpochMilli());
    }
    
    private String generateSignature(String bodyString, String nonce) throws Exception {
        String stringToSign = nonce + "." + bodyString;
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] bytes = sha256_HMAC.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private Map<String, Object> sendRequest(String endpoint, Object payload, Map<String, String> extraHeaders) throws Exception {
        String nonce = getNonce();
        String bodyString = mapper.writeValueAsString(payload); // Jackson produces compact JSON by default
        String signature = generateSignature(bodyString, nonce);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + publishableKey)
                .header("X-Mmpay-Nonce", nonce)
                .header("X-Mmpay-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(bodyString));

        if (extraHeaders != null) {
            extraHeaders.forEach(builder::header);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("API Error: " + response.statusCode() + " " + response.body());
        }

        return mapper.readValue(response.body(), Map.class);
    }

    // --- Sandbox Methods ---

    public Map<String, Object> sandboxHandShake(String orderId, String nonce) throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("nonce", nonce);

        Map<String, Object> response = sendRequest("/payments/sandbox-handshake", payload, null);
        
        if (response.containsKey("token")) {
            this.btoken = (String) response.get("token");
        }
        return response;
    }

    public Map<String, Object> sandboxPay(PaymentRequest params) throws Exception {
        String nonce = getNonce();

        ObjectNode xPayload = mapper.valueToTree(params);
        xPayload.put("appId", this.appId);
        xPayload.put("nonce", nonce);

        // 1. Handshake
        sandboxHandShake(params.orderId, nonce);

        // 2. Pay
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Mmpay-Btoken", this.btoken);

        return sendRequest("/payments/sandbox-create", xPayload, headers);
    }

    public Map<String, Object> sandboxGet(PayGetRequest params) throws Exception {
        String nonce = getNonce();

        ObjectNode xPayload = mapper.valueToTree(params);
        xPayload.put("nonce", nonce);

        // 1. Handshake
        sandboxHandShake(params.orderId, nonce);

        // 2. Get
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Mmpay-Btoken", this.btoken);

        return sendRequest("/payments/sandbox-get", xPayload, headers);
    }

    // --- Production Methods ---

    public Map<String, Object> handShake(String orderId, String nonce) throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("nonce", nonce);

        Map<String, Object> response = sendRequest("/payments/handshake", payload, null);
        
        if (response.containsKey("token")) {
            this.btoken = (String) response.get("token");
        }
        return response;
    }

    public Map<String, Object> pay(PaymentRequest params) throws Exception {
        String nonce = getNonce();

        ObjectNode xPayload = mapper.valueToTree(params);
        xPayload.put("appId", this.appId);
        xPayload.put("nonce", nonce);

        // 1. Handshake
        handShake(params.orderId, nonce);

        // 2. Pay
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Mmpay-Btoken", this.btoken);

        return sendRequest("/payments/create", xPayload, headers);
    }

    public Map<String, Object> get(PayGetRequest params) throws Exception {
        String nonce = getNonce();

        ObjectNode xPayload = mapper.valueToTree(params);
        xPayload.put("nonce", nonce);

        // 1. Handshake
        handShake(params.orderId, nonce);

        // 2. Get
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Mmpay-Btoken", this.btoken);

        return sendRequest("/payments/get", xPayload, headers);
    }

    // --- Verification ---

    public boolean verifyCb(String payload, String nonce, String expectedSignature) {
        try {
            if (payload == null || nonce == null || expectedSignature == null) return false;
            String generatedSignature = generateSignature(payload, nonce);
            return generatedSignature.equals(expectedSignature);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}