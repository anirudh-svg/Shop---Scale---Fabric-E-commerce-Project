package com.shopscale.cart.dto;

import java.math.BigDecimal;

/**
 * Response from Price Service
 */
public class PriceResponse {

    private String productId;
    private String productName;
    private BigDecimal price;
    private String currency;

    // Constructors
    public PriceResponse() {
    }

    public PriceResponse(String productId, String productName, BigDecimal price, String currency) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.currency = currency;
    }

    // Getters and Setters
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
