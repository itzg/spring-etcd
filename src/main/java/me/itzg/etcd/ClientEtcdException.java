package me.itzg.etcd;

import me.itzg.etcd.keys.Response;

/**
 * @author Geoff Bourne
 * @since 6/19/2015
 */
public class ClientEtcdException extends EtcdException {
    private final String cause;
    private final int errorCode;
    private final int index;

    protected ClientEtcdException(String message, String cause, int errorCode, int index) {
        super(message);
        this.cause = cause;
        this.errorCode = errorCode;
        this.index = index;
    }

    public static ClientEtcdException buildFrom(Response response) {
        return new ClientEtcdException(response.getMessage(), response.getCause(), response.getErrorCode(), response.getIndex());
    }

    public String getEtcdCause() {
        return cause;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public int getIndex() {
        return index;
    }
}
