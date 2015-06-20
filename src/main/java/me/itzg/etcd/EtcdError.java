package me.itzg.etcd;

/**
 * Encapsulates <a href="https://github.com/coreos/etcd/blob/master/Documentation/errorcode.md">standard v2 errors</a>
 * @author Geoff Bourne
 * @since 6/19/2015
 */
public enum EtcdError {
    EcodeKeyNotFound(100, "Key not found", EtcdErrorType.Command),
    EcodeTestFailed(101, "Compare failed", EtcdErrorType.Command),
    EcodeNotFile(102, "Not a file", EtcdErrorType.Command),
    EcodeNotDir(104, "Not a directory", EtcdErrorType.Command),
    EcodeNodeExist(105, "Key already exists", EtcdErrorType.Command),
    EcodeRootROnly(107, "Root is read only", EtcdErrorType.Command),
    EcodeDirNotEmpty(108, "Directory not empty", EtcdErrorType.Command),

    EcodePrevValueRequired(201, "PrevValue is Required in POST form", EtcdErrorType.PostForm),
    EcodeTTLNaN(202, "The given TTL in POST form is not a number", EtcdErrorType.PostForm),
    EcodeIndexNaN(203, "The given index in POST form is not a number", EtcdErrorType.PostForm),
    EcodeInvalidField(209, "Invalid field", EtcdErrorType.PostForm),
    EcodeInvalidForm(210, "Invalid POST form", EtcdErrorType.PostForm),

    EcodeRaftInternal(300, "Raft Internal Error", EtcdErrorType.Raft),
    EcodeLeaderElect(301, "During Leader Election", EtcdErrorType.Raft),

    EcodeWatcherCleared(400, "watcher is cleared due to etcd recovery", EtcdErrorType.Etcd),
    EcodeEventIndexCleared(401, "The event in requested index is outdated and cleared", EtcdErrorType.Etcd),

    NotAnError(0, "", null),
    EcodeUnknown(-1, "Unknown", EtcdErrorType.Command);

    private final int code;
    private final String strerror;
    private final EtcdErrorType type;

    EtcdError(int code, String strerror, EtcdErrorType type) {
        this.code = code;
        this.strerror = strerror;
        this.type = type;
    }

    public static EtcdError resolve(int code) {
        for (EtcdError etcdError : EtcdError.values()) {
            if (etcdError.getCode() == code) {
                return etcdError;
            }
        }
        return EcodeUnknown;
    }

    public int getCode() {
        return code;
    }

    public String getStrerror() {
        return strerror;
    }

    public EtcdErrorType getType() {
        return type;
    }
}
