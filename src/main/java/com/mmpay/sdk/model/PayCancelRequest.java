package com.mmpay.sdk.model;

public class PayCancelRequest {
    public String orderId;

    public PayCancelRequest() {}

    public PayCancelRequest(String orderId) {
        this.orderId = orderId;
    }
}