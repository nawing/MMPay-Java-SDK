# MMPay Java SDK

A comprehensive Java client library for integrating with the MMPay Payment Gateway. Built with modern Java (11+) and Jackson, it provides robust utilities for payment creation, handshake authentication, and secure webhook verification.

## 📦 Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.mmpay</groupId>
    <artifactId>mmpay-java-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

You also need **Jackson** for JSON processing:

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>
```

## 🚀 Configuration

Initialize the SDK with your credentials.

```java
import com.mmpay.sdk.MMPaySDK;
import com.mmpay.sdk.SDKOptions;

SDKOptions options = new SDKOptions(
    "YOUR_APP_ID",
    "YOUR_PUBLISHABLE_KEY",
    "YOUR_SECRET_KEY",
    "[https://api.mmpay.com](https://api.mmpay.com)"
);

MMPaySDK sdk = new MMPaySDK(options);
```

### Configuration Parameters

| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `appId` | `String` | **Yes** | Your unique Application ID. |
| `publishableKey` | `String` | **Yes** | Public key used for identification. |
| `secretKey` | `String` | **Yes** | Private key used for HMAC SHA256 signing. |
| `apiBaseUrl` | `String` | **Yes** | The base URL for the MMPay API. |

---

## 🛠 Usage

### 1. Create a Payment (Sandbox)

Use `sandboxPay` for testing.

```java
import com.mmpay.sdk.model.PaymentRequest;
import com.mmpay.sdk.model.Item;
import java.util.List;
import java.util.ArrayList;

try {
    PaymentRequest request = new PaymentRequest();
    request.orderId = "ORD-SANDBOX-" + System.currentTimeMillis();
    request.amount = 5000;
    request.currency = "MMK";
    request.callbackUrl = "[https://yoursite.com/webhook](https://yoursite.com/webhook)";
    
    // Add Items
    request.items = new ArrayList<>();
    request.items.add(new Item("Premium Plan", 5000, 1));

    Map<String, Object> response = sdk.sandboxPay(request);
    System.out.println("Response: " + response);

} catch (Exception e) {
    e.printStackTrace();
}
```

#### Parameters: `PaymentRequest`

| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `orderId` | `String` | **Yes** | Unique identifier for this order. |
| `amount` | `double` | **Yes** | Total transaction amount. |
| `items` | `List<Item>` | **Yes** | List of items. |
| `currency` | `String` | No | Currency code. |
| `callbackUrl` | `String` | No | Webhook URL. |

---

### 2. Create a Payment (Production)

Switch to `pay()` for live transactions.

```java
try {
    PaymentRequest request = new PaymentRequest();
    request.orderId = "ORD-LIVE-999";
    request.amount = 10000;
    request.items = List.of(new Item("E-Commerce Goods", 10000, 1));

    Map<String, Object> response = sdk.pay(request);
    
    // Redirect user
    String paymentUrl = (String) response.get("url");
    System.out.println("Payment URL: " + paymentUrl);

} catch (Exception e) {
    e.printStackTrace();
}
```

---

### 3. Verify Webhook (Callback)

Secure your application by verifying incoming webhooks.

```java
// Example in a Spring Boot Controller
@PostMapping("/webhook")
public ResponseEntity<String> handleWebhook(
    @RequestBody String payload, 
    @RequestHeader("X-Mmpay-Nonce") String nonce,
    @RequestHeader("X-Mmpay-Signature") String signature
) {
    
    boolean isValid = sdk.verifyCb(payload, nonce, signature);

    if (isValid) {
        // ✅ Process order
        return ResponseEntity.ok("Verified");
    } else {
        // ❌ Invalid signature
        return ResponseEntity.status(400).body("Invalid Signature");
    }
}
```

#### Parameters: `verifyCb`

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `payload` | `String` | The raw JSON string body of the request. |
| `nonce` | `String` | Value of the `X-Mmpay-Nonce` header. |
| `expectedSignature` | `String` | Value of the `X-Mmpay-Signature` header. |

## 📄 License

MIT License.