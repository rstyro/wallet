package com.hereblock.wallet.provider.exception;

public class WalletException extends RuntimeException {
    public WalletException() {
        super();
    }

    public WalletException(String message) {
        super(message);
    }

    protected WalletException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public WalletException(String message, Throwable cause) {
        super(message, cause);
    }

    public WalletException(Throwable cause) {
        super(cause);
    }
}
