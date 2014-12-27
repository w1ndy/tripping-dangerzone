package kvstore;

public class KVException extends Exception {

    private final KVMessage kvm;

    private static final long serialVersionUID = 1L;

    /**
     * Construct a KVException with a particular KVMessage.
     *
     * @param kvm KVMessage for this KVException
     */
    public KVException(KVMessage kvm) {
        this.kvm = kvm;
    }

    /**
     * Construct a KVException with the provided error string.
     *
     * @param errorMessage String describing the error
     */
    public KVException(String errorMessage) {
        this.kvm = new KVMessage(KVConstants.RESP, errorMessage);
    }

    /**
     * Getter for the inner KVMessage containing the error message.
     *
     * @return KVMessage containing error message
     */
    public final KVMessage getKVMessage() {
        return kvm;
    }

    @Override
    public String getMessage() {
        return this.getKVMessage().getMessage();
    }
}
