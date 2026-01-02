package com.mmpay.sdk.model;

// 'public' class matches TypeScript's 'export interface'
public class Item {
    public String name;
    public double amount;
    public int quantity;

    // Default constructor for JSON serialization
    public Item() {}

    public Item(String name, double amount, int quantity) {
        this.name = name;
        this.amount = amount;
        this.quantity = quantity;
    }
}