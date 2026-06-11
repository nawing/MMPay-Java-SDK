// File: MMPay-Java-SDK/src/main/java/com/mmpay/sdk/MMPaySDK.java

package com.mmpay.sdk;

import com.mmpay.sdk.model.Item;
import com.mmpay.sdk.model.PaymentRequest;
import com.mmpay.sdk.model.PayGetRequest;
import com.mmpay.sdk.model.PayCancelRequest;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// --- Main SDK Class ---
public class MMPaySDK {

    private final String appId;
    private final String publishableKey;
    private final String secretKey;
    private final String apiBaseUrl;
    private final boolean isSandbox;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private String btoken;

    // Event listeners
    private final Map<String, List<Consumer<Object>>> listeners = new HashMap<>();

    public MMPaySDK(SDKOptions options) {
        this.appId = options.appId;
        this.publishableKey = options.publishableKey;
        this.secretKey = options.secretKey;
        this.apiBaseUrl = options.apiBaseUrl.replaceAll("/$", ""); // Remove trailing slash
        this.isSandbox = this.publishableKey.contains("_test_") || this.secretKey.contains("_test_");
        
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

        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = mapper.readValue(response.body(), Map.class);
        return responseMap;
    }

    // --- Event Emitter ---

    public MMPaySDK on(String event, Consumer<Object> callback) {
        listeners.computeIfAbsent(event, k -> new ArrayList<>()).add(callback);
        return this;
    }

    private void emit(String event, Object data) {
        List<Consumer<Object>> eventListeners = listeners.get(event);
        if (eventListeners != null) {
            for (Consumer<Object> listener : eventListeners) {
                listener.accept(data);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public MMPaySDK onTxCreate(Consumer<Map<String, Object>> cb) { return on("tx:create", obj -> cb.accept((Map<String, Object>) obj)); }
    @SuppressWarnings("unchecked")
    public MMPaySDK onTxSuccess(Consumer<Map<String, Object>> cb) { return on("tx:success", obj -> cb.accept((Map<String, Object>) obj)); }
    @SuppressWarnings("unchecked")
    public MMPaySDK onTxFail(Consumer<Map<String, Object>> cb) { return on("tx:failed", obj -> cb.accept((Map<String, Object>) obj)); }
    @SuppressWarnings("unchecked")
    public MMPaySDK onTxRefund(Consumer<Map<String, Object>> cb) { return on("tx:refunded", obj -> cb.accept((Map<String, Object>) obj)); }
    @SuppressWarnings("unchecked")
    public MMPaySDK onTxCancel(Consumer<Map<String, Object>> cb) { return on("tx:cancel", obj -> cb.accept((Map<String, Object>) obj)); }
    @SuppressWarnings("unchecked")
    public MMPaySDK onTxExpire(Consumer<Map<String, Object>> cb) { return on("tx:expire", obj -> cb.accept((Map<String, Object>) obj)); }
    @SuppressWarnings("unchecked")
    public MMPaySDK onHeartbeat(Consumer<Map<String, Object>> cb) { return on("tx:heartbeat", obj -> cb.accept((Map<String, Object>) obj)); }
    public MMPaySDK onError(Consumer<Exception> cb) { return on("error", obj -> cb.accept((Exception) obj)); }

    // --- Methods ---

    public Map<String, Object> handShake(String orderId, String nonce) throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("orderId", orderId);
        payload.put("nonce", nonce);

        String segment = this.isSandbox ? "sandbox-handshake" : "handshake";
        Map<String, Object> response = sendRequest("/payments/" + segment, payload, null);
        
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

        String segment = this.isSandbox ? "sandbox-create" : "create";
        return sendRequest("/payments/" + segment, xPayload, headers);
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

        String segment = this.isSandbox ? "sandbox-get" : "get";
        return sendRequest("/payments/" + segment, xPayload, headers);
    }

    public Map<String, Object> cancel(PayCancelRequest params) throws Exception {
        String nonce = getNonce();

        ObjectNode xPayload = mapper.valueToTree(params);
        xPayload.put("nonce", nonce);

        // 1. Handshake
        handShake(params.orderId, nonce);

        // 2. Cancel
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Mmpay-Btoken", this.btoken);

        String segment = this.isSandbox ? "sandbox-cancel" : "cancel";
        return sendRequest("/payments/" + segment, xPayload, headers);
    }

    // --- Verification & Listening ---

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

    public MMPaySDK listen(String payload, String nonce, String expectedSignature) {
        try {
            if (!verifyCb(payload, nonce, expectedSignature)) {
                throw new RuntimeException("Signature verification failed");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> tx = mapper.readValue(payload, Map.class);
            String status = (String) tx.get("status");
            String condition = (String) tx.get("condition");

            if ("PENDING".equals(status)) {
                emit("tx:create", tx);
            } else if ("SUCCESS".equals(status)) {
                if ("TOUCHED".equals(condition)) {
                    emit("tx:heartbeat", tx);
                } else {
                    emit("tx:success", tx);
                }
            } else if ("FAILED".equals(status)) {
                emit("tx:failed", tx);
            } else if ("REFUNDED".equals(status)) {
                emit("tx:refunded", tx);
            } else if ("CANCELLED".equals(status)) {
                emit("tx:cancel", tx);
            } else if ("EXPIRED".equals(status)) {
                emit("tx:expire", tx);
            } else {
                emit("tx:unknown", tx);
            }
        } catch (Exception e) {
            emit("error", e);
        }

        return this;
    }
}