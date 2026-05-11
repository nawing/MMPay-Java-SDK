# MMPay Java SDK

A Java client library for integrating with the MMPay Payment Gateway. This SDK provides utilities for payment creation, transaction retrieval, handshake authentication, and callback verification.

## Features

- Sandbox & Production Support: Dedicated methods for both environments.
- Payment Creation & Retrieval: Endpoints to create payments and fetch transaction statuses.
- HMAC SHA256 Signing: Automatic signature generation for request integrity.
- Callback Verification: Utility to verify incoming webhooks from MMPay.
- Object Models: Strongly typed request/response classes.

## Installation

Add the dependency to your pom.xml (Maven):

```xml 
<dependency>     
    <groupId>com.mmpay</groupId>     
    <artifactId>mmpay-java-sdk</artifactId>
    <version>1.0.1</version> 
</dependency> 
```

## Configuration

To use the SDK, you need your App ID, Publishable Key, and Secret Key provided by the MMPay dashboard.

| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `appId` | String | **Yes** | Your unique Application ID. |
| `publishableKey` | String | **Yes** | Public key for authentication. |
| `secretKey` | String | **Yes** | Private key used for signing requests (HMAC). |
| `apiBaseUrl` | String | **Yes** | The base URL for the MMPay API. |

```java 
import com.mmpay.sdk.MMPaySDK; 
import com.mmpay.sdk.model.SDKOptions;  
SDKOptions options = new SDKOptions(
    "YOUR_APP_ID",
    "YOUR_PUBLISHABLE_KEY", 
    "YOUR_SECRET_KEY",
    "https://xxx.myanmyanpay.com" 
);  
MMPaySDK sdk = new MMPaySDK(options); 
```
## Usage

### 1. Payment Request Payload

Passed to sdk.pay(params) or sdk.sandboxPay(params).

| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `orderId` | String | **Yes** | Unique identifier for the order (e.g., "ORD-001"). |
| `amount` | double | **Yes** | Total transaction amount. |
| `items` | List<Item> | No | A list of items included in the order. |
| `callbackUrl` | String | No | URL where the webhook callback will be sent. |
| `customMessage` | String | No | Custom message to be attached to the transaction. |

Item Object

Used inside the items list of a Payment Request.

| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `name` | String | **Yes** | Name of the product/service. |
| `amount` | double | **Yes** | Price per unit. |
| `quantity` | int | **Yes** | Quantity of the item. |

### 2. Create a Payment (Sandbox)

```java 
import com.mmpay.sdk.model.PaymentRequest; 
import com.mmpay.sdk.model.Item; 
import java.util.Arrays; 
import java.util.Map;

try {
    PaymentRequest request = new PaymentRequest();
    request.orderId = "ORD-SANDBOX-001";
    request.amount = 5000.0;
    request.callbackUrl = "https://your-site.com/webhook/mmpay";
    request.customMessage = "Your Custom Message";

    Item item = new Item("Premium Subscription", 5000.0, 1);
    request.items = Arrays.asList(item);

    Map<String, Object> response = sdk.sandboxPay(request);
    System.out.println(response);  

} catch (Exception e) {     
    e.printStackTrace(); 
} 
```
### 3. Retrieve a Payment

```java 
// Sandbox Environment
import com.mmpay.sdk.model.PayGetRequest; 
import java.util.Map;

try {
    PayGetRequest getRequest = new PayGetRequest("ORD-SANDBOX-001");

    Map<String, Object> response = sdk.sandboxGet(getRequest);
    System.out.println("Status: " + response.get("status"));

} catch (Exception e) {
    e.printStackTrace(); 
} 
```

```java 
// Production Environment
import com.mmpay.sdk.model.PayGetRequest; 
import java.util.Map;

try {
    PayGetRequest getRequest = new PayGetRequest("ORD-PRODUCTION-001");

    Map<String, Object> response = sdk.get(getRequest);
    System.out.println("Status: " + response.get("status"));

} catch (Exception e) {
    e.printStackTrace(); 
} 
```

### 4. Create a Payment (Production)

```java 
import com.mmpay.sdk.model.PaymentRequest; 
import com.mmpay.sdk.model.Item; 
import java.util.Arrays; 
import java.util.Map;  

try {     
    PaymentRequest request = new PaymentRequest();     
    request.orderId = "ORD-LIVE-98765";
    request.amount = 5000.0;
    request.callbackUrl = "https://your-site.com/webhook/mmpay";
    request.customMessage = "Your Custom Message";

    Item item = new Item("Premium Subscription", 5000.0, 1);     
    request.items = Arrays.asList(item);

    Map<String, Object> response = sdk.pay(request);
    System.out.println("Payment URL: " + response.get("url"));

} catch (Exception e) {     
    e.printStackTrace(); 
} 
```
### 5. Verify Callback (Webhook)

When MMPay sends a callback to your callbackUrl, you must verify the request signature to ensure it is genuine.

Example Verification (Spring Boot)

```java 
import org.springframework.web.bind.annotation.*; 
import org.springframework.http.ResponseEntity;  

@RestController @RequestMapping("/webhook") 
public class WebhookController {      
    @PostMapping("/mmpay")     
    public ResponseEntity<String> handleMMPayWebhook(             
        @RequestBody String payload, 
        @RequestHeader("X-Mmpay-Nonce") String nonce,
        @RequestHeader("X-Mmpay-Signature") String signature
    ) {          
            // Assuming 'sdk' is injected or globally available         
        boolean isValid = sdk.verifyCb(payload, nonce, signature);          
        if (isValid) {       
            // Process the order securely        
            return ResponseEntity.ok("Verified");         
        } else {         
            return ResponseEntity.badRequest().body("Invalid Signature"); 
        }    
    } 
} 
```


---

## Error Codes

##### Api Key Layer Authentication [SERVER SDK]
| Code | Description |
| :--- | :--- |
| **`KA0001`** | Bearer Token Not Included In Your Request |
| **`KA0002`** | API Key Not 'LIVE' |
| **`KA0003`** | Signature mismatch |
| **`KA0004`** | Internal Server Error ( Talk to our support immediately fot this ) |
| **`KA0005`** | IP Not whitelisted |
| **`429`** | Ratelimit hit only 1000 request / minute allowed |


##### JWT Layer Authentication [SERVER SDK]
| Code | Description |
| :--- | :--- |
| **`BA001`** | `Btoken` is nonce one time token is not included |
| **`BA002`** | `Btoken` one time nonce mismatch |
| **`BA000`** | Internal Server Error ( Talk to our support immediately fot this ) |
| **`429`**   | Ratelimit hit only 1000 request / minute allowed |


---

## 📄 License

MIT License.