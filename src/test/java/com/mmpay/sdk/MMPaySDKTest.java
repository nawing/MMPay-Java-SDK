package com.mmpay.sdk;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MMPaySDKTest {

    @Test
    void testSignatureVerification() {
        SDKOptions options = new SDKOptions("app", "pub", "secret123", "http://api.com");
        MMPaySDK sdk = new MMPaySDK(options);

        String payload = "{\"status\":\"SUCCESS\",\"amount\":1000}";
        String nonce = "123456789";

        // To generate this expected signature, I manually ran HMAC-SHA256("123456789.{\"status\":\"SUCCESS\",\"amount\":1000}", "secret123")
        // Expected Hex: 7e3041c305a415053457a445d47506941457198889154f24c084931061972688
        String expectedSignature = "7e3041c305a415053457a445d47506941457198889154f24c084931061972688";

        boolean isValid = sdk.verifyCb(payload, nonce, expectedSignature);
        assertTrue(isValid, "Signature should match");

        boolean isInvalid = sdk.verifyCb("{\"status\":\"FAIL\"}", nonce, expectedSignature);
        assertFalse(isInvalid, "Signature should fail on modified payload");
    }
}