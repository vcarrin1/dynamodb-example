package com.vcarrin87.dynamodb_example.models;

public final class Keys {
    private Keys() {}

    public static String customerPk(String customerId) {
        return "CUSTOMER#" + customerId;
    }

    public static String customerProfileSk() {
        return "PROFILE";
    }

    public static String customerOrderSk(String orderId) {
        return "ORDER#" + orderId;
    }

    public static String orderPk(String orderId) {
        return "ORDER#" + orderId;
    }

    public static String orderItemSk(String orderItemId) {
        return "ITEM#" + orderItemId;
    }

    public static String paymentSk(String paymentId) {
        return "PAYMENT#" + paymentId;
    }

    public static String productPk(String productId) {
        return "PRODUCT#" + productId;
    }

    public static String productDetailSk() {
        return "DETAIL";
    }

    public static String inventorySk() {
        return "INVENTORY";
    }
}
