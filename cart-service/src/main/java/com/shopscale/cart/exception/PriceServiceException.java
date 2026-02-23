package com.shopscale.cart.exception;

public class PriceServiceException extends RuntimeException {

    public PriceServiceException(String message) {
        super(message);
    }

    public PriceServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
