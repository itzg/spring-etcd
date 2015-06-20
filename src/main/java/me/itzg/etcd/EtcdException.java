package me.itzg.etcd;

/**
 * @author Geoff Bourne
 * @since 6/19/2015
 */
public class EtcdException extends Exception {
    private final EtcdError etcdError;

    public EtcdException() {
        etcdError = null;
    }

    public EtcdException(String message) {
        this(message, null);
    }

    public EtcdException(String message, EtcdError etcdError) {
        super(message);
        this.etcdError = etcdError;
    }

    public EtcdError getEtcdError() {
        return etcdError;
    }
}
