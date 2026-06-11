# MMPay Java SDK

A Java client library for integrating with the MMPay Payment Gateway. This SDK provides utilities for payment creation, transaction retrieval, handshake authentication, and callback verification.

## Features

- Sandbox & Production Support: Dedicated methods for both environments.
- Payment Creation & Retrieval: Endpoints to create payments and fetch transaction statuses.
- HMAC SHA256 Signing: Automatic signature generation for request integrity.
- Callback Verification: Utility to verify incoming webhooks from MMPay.
- Object Models: Strongly typed request/response classes.

## ⬇️ 1. Installation

Add the dependency to your pom.xml (Maven):

```xml 
<dependency>     
    <groupId>com.mmpay</groupId>     
    <artifactId>mmpay-java-sdk</artifactId>
    <version>1.0.1</version> 
</dependency> 
```

----

## ⚙️ 2. Configuration

To use the SDK, you need your App ID, Publishable Key, and Secret Key provided by the MMPay dashboard.

| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `appId` | String | **Yes** | Your unique Application ID. |
| `publishableKey` | String | **Yes** | Public key for authentication. |
| `secretKey` | String | **Yes** | Private key used for signing requests (HMAC). |
| `apiBaseUrl` | String | **Yes** | The base URL for the MMPay API. |

**Implementation**

```java 
import com.mmpay.sdk.MMPaySDK; 
import com.mmpay.sdk.model.SDKOptions;  
SDKOptions options = new SDKOptions(
    "YOUR_APP_ID",
    "YOUR_PUBLISHABLE_KEY", 
    "YOUR_SECRET_KEY",
    "https://xxx.myanmyanpay.com" 
);  
MMPaySDK mmpayX = new MMPaySDK(options); 
```

----

## 💳 3. Make Payment

**Method Signature**

```java
pay(PaymentRequest: payRequest)
```

**Implementation**

```java 
import com.mmpay.sdk.model.PaymentRequest; 
import com.mmpay.sdk.model.Item; 
import java.util.Arrays; 
import java.util.Map;

try {
    PaymentRequest payRequest = new PaymentRequest();
    payRequest.orderId = "ORD-SANDBOX-001";
    payRequest.amount = 5000.0;
    payRequest.callbackUrl = "https://your-site.com/webhook/mmpay";
    payRequest.customMessage = "Your Custom Message";

    Item item = new Item("Premium Subscription", 5000.0, 1);
    payRequest.items = Arrays.asList(item);

    Map<String, Object> response = mmpayX.pay(payRequest);
    System.out.println(response);  

} catch (Exception e) {     
    e.printStackTrace(); 
} 
```

**Request Body** (`payload` structure)

| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `orderId` | String | **Yes** | Unique identifier for the order (e.g., "ORD-001"). |
| `amount` | double | **Yes** | Total transaction amount. |
| `items` | List<Item> | No | A list of items included in the order. |
| `callbackUrl` | String | No | URL where the webhook callback will be sent. |
| `customMessage` | String | No | Custom message to be attached to the transaction. |

**Item Object**

| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `name` | String | **Yes** | Name of the product/service. |
| `amount` | double | **Yes** | Price per unit. |
| `quantity` | int | **Yes** | Quantity of the item. |

**Response Body** Code (`201`)

```json
{
  "orderId": "_trx_0012345",
  "status": "PENDING",
  "vendorQrRefId": "39233043003345",
  "transactionRefId": "39233043003345", // This is deprecated - transactionRefId will show only after payment is confirmed
  "amount": 2800,
  "currency": "MMK",
  "qr": "EMVco MMQR String => You_have_to_embed_as_qr_image_yourself"
}
```

----

### 4. Retrieve a Payment Information

**Method Signature**

```java
get(PayGetRequest getRequest)
```

**Implementation**

```java 
import com.mmpay.sdk.model.PayGetRequest; 
import java.util.Map;

try {
    PayGetRequest getRequest = new PayGetRequest("ORD-SANDBOX-001");

    Map<String, Object> response = mmpayX.get(getRequest);
    System.out.println("Status: " + response.get("status"));

} catch (Exception e) {
    e.printStackTrace(); 
} 
```

**Request Body** (`payload` structure)

| Field | Type | Required | Description | Example |
| :--- | :--- | :--- | :--- | :--- |
| **`orderId`**         | `string` | **Yes**    | Your generated order ID for the order or system initiating the payment. | `"ORD-3983833"` |

**Response Body** Code (`200`)

```json
{
  "orderId": "ORD-111111111",
  "appId": "MMP3883483",
  "amount": 1000,
  "vendor": "KBZPay",
  "method": "QR",
  "customMessage": "",
  "callbackUrl": "",
  "callbackUrlAt": "JSDateObject",
  "callbackUrlStatus": "SUCCESS",
  "status": "SUCCESS", //  'PENDING' | 'SUCCESS' | 'FAILED' | 'REFUNDED' | 'CANCELLED' | 'EXPIRED';
  "disbursementId": "289348734939",
  "disStatus": "SUCCESS",
  "condition": "TOUCHED", // TOUCHED | 'PRISTINE' | 'DIRTY' | 'EXPIRED'
  "createdAt": "JSDateObject",
  "transactionRefId": "939583046594",
  "vendorQrRefId": "48309449034",
  "qr": "EMVCo QR String::MMQR Standard",
}
```

----

### 5. Cancel a Payment

**Method Signature**

```java
cancel(PayCancelRequest cancelRequest)
```

**Implementation**

```java 
import com.mmpay.sdk.model.PayCancelRequest; 
import java.util.Map;

try {
    PayCancelRequest cancelRequest = new PayCancelRequest("ORD-SANDBOX-001");

    Map<String, Object> response = mmpayX.cancel(cancelRequest);
    System.out.println("Status: " + response.get("status"));

} catch (Exception e) {
    e.printStackTrace(); 
} 
```

**Request Body** (`payload` structure)

| Field | Type | Required | Description | Example |
| :--- | :--- | :--- | :--- | :--- |
| **`orderId`**         | `string` | **Yes**    | Your generated order ID for the order or system initiating the payment. | `"ORD-3983833"` |

**Response Body** Code (`200`)

```json
{
  "amount": 1000,
  "orderId": "ORD-111111111",
  "status": "CANCELLED",
  "vendorQrRefId": "289348734939",
}
```

----

## 🔐 6. Handling Webhooks
To secure your webhook endpoint that receives callbacks from the MMPay server, use this event listener to handle the events.
The **listen** performs the mandatory Signature and Nonce verification and emits events

**Incoming Headers**

| Field Name | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| **Content-Type** | `string` | Yes | 'application/json' |
| **X-Mmpay-Signature** | `string` | Yes | '34834890vfgh9hnf94irfg_48932i4rt90349849' |
| **X-Mmpay-Nonce** | `string` | Yes | '94843943949349' |

**Incoming Body**

| Field Name    | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| **orderId**           | `string` | Yes | Unique identifier for the specific order. |
| **amount**            | `number` | Yes | The transaction amount. |
| **currency**          | `string` | Yes | The 3-letter currency code (e.g., MMK, USD). |
| **vendor**            | `string` | Yes | Identifier for the vendor initiating the request. |
| **method**            | `'QR', 'PIN', 'PWA', 'CARD'`  | Yes | Identifier for the method. |
| **status**            | `'PENDING','SUCCESS','FAILED','REFUNDED', 'EXPIRED', 'CANCELLED'` | Yes | Current status of the transaction. |
| **condition**         | `'PRESTINE', 'TOUCHED', 'EXPIRED', 'DIRTY'`  | Yes | Used QR Code scan again or not |
| **transactionRefId**  | `string` | Yes | The reference ID generated by the payment provider after success payment |
| **vendorQrRefId**     | `string` | Yes | The MMQR reference ID generated by the payment provider. |
| **callbackUrl**       | `string` | No | Optional URL to receive webhooks or updates. |
| **customMessage**     | `string` | No | User provided custom message |


**Implementation With Spring Boot Framework**

```java 
package com.yourcompany.app.config;

import com.mmpay.sdk.MMPaySDK;
import com.mmpay.sdk.model.SDKOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MMPayConfig {

    private static final Logger log = LoggerFactory.getLogger(MMPayConfig.class);

    @Value("${mmpay.app-id}")
    private String appId;

    @Value("${mmpay.publishable-key}")
    private String publishableKey;

    @Value("${mmpay.secret-key}")
    private String secretKey;

    @Value("${mmpay.api-base-url:https://api.myanmyanpay.com}")
    private String apiBaseUrl;

    @Bean
    public MMPaySDK mmPaySDK() {
        SDKOptions options = new SDKOptions();
        options.appId = appId;
        options.publishableKey = publishableKey;
        options.secretKey = secretKey;
        options.apiBaseUrl = apiBaseUrl;

        MMPaySDK mmpayX = new MMPaySDK(options);

        // Attach listeners exactly ONCE during application startup
        mmpayX.onTxCreate(tx -> {
            log.info("Payment Created for order: {}", tx.get("orderId"));
        }).onTxSuccess(tx -> {
            // Update your database order status here
            log.info("Payment Successful for order: {}", tx.get("orderId"));
        }).onTxFail(tx -> {
            log.error("Payment Failed for order: {}", tx.get("orderId"));
        }).onTxCancel(tx -> {
            log.info("Payment Cancelled for order: {}", tx.get("orderId"));
        }).onTxExpire(tx -> {
            log.info("Payment Expired for order: {}", tx.get("orderId"));
        }).onHeartbeat(tx -> {
            log.info("Heartbeat received for order: {}", tx.get("orderId"));
        }).onError(error -> {
            log.error("MMPay SDK Error: ", error);
        });
        return mmpayX;
    }
}
```

```java
package com.yourcompany.app.controller;

import com.mmpay.sdk.MMPaySDK;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class MMPayWebhookController {

    private final MMPaySDK mmPaySDK;

    // Spring automatically injects the Singleton bean we defined above
    public MMPayWebhookController(MMPaySDK mmPaySDK) {
        this.mmPaySDK = mmPaySDK;
    }

    @PostMapping("/mmpay")
    public ResponseEntity<Map<String, Boolean>> handleMMPayWebhook(
            @RequestHeader("X-Mmpay-Nonce") String nonce,
            @RequestHeader("X-Mmpay-Signature") String signature,
            @RequestBody String payloadString) {

        try {
            // The listen method verifies the signature and triggers the correct event listener
            mmPaySDK.listen(payloadString, nonce, signature);
            return ResponseEntity.ok(Map.of("received", true));
            
        } catch (Exception e) {
            // If listen() throws an error (e.g., signature mismatch)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body(Map.of("received", false));
        }
    }
}
```


---

## 💡 7. Error Codes

**HMac Layer (SERVER Side)**

| Code | Description |
| :--- | :--- |
| `KA0001` | Bearer Token Not Included |
| `KA0002` | API Key Not 'LIVE' |
| `KA0003` | Signature mismatch |
| `KA0004` | Internal Server Error |
| `KA0005` | IP Not whitelisted |
| `429` | Rate limit hit (1000 req/min) |

**JWT Layer (SERVER Side)**

| Code | Description |
| :--- | :--- |
| `BA001` | `Btoken` nonce token missing |
| `BA002` | `Btoken` nonce mismatch |


**Response Codes**

| Code | Status | Description |
| :--- | :--- | :--- |
| **`201`** | Created | Transaction initiated successfully. Response contains QR code URL/details. |
| **`401`** | Unauthorized | Invalid or missing Publishable Key. |
| **`400`** | Bad Request | Missing required body fields (validated by schema, if implemented). |
| **`503`** | Service Unavailable | Upstream payment API failed or is unreachable. |
| **`500`** | Internal Server Error | General server error during payment initiation. |


---

## 📄 License

MIT License.