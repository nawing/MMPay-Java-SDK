package com.mmpay.sdk.model;

import java.util.List;

public class PaymentRequest {
    public String orderId;
    public double amount;
    public String currency;
    public String callbackUrl;
    public String customMessage; // Added to match Node/PHP implementations
    
    public List<Item> items; 
}