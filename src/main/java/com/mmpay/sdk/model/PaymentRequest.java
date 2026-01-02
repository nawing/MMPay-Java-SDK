package com.mmpay.sdk.model;

import java.util.List;

public class PaymentRequest {
    public String orderId;
    public double amount;
    public String currency;
    public String callbackUrl;
    
    // We use the Item class defined in the same package
    public List<Item> items; 

    // Optional: Add a constructor or builder pattern here if you like
}