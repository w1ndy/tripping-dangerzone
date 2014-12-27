package kvstore;

public class KVConstants {

    // general constants used for proj3 and proj4
    public static final String GET_REQ  = "getreq";
    public static final String PUT_REQ  = "putreq";
    public static final String DEL_REQ  = "delreq";
    public static final String RESP     = "resp";
    public static final String SUCCESS  = "Success";

    // proj4-specific constants
    public static final String READY    = "ready";
    public static final String REGISTER = "register";
    public static final String ABORT    = "abort";
    public static final String COMMIT   = "commit";
    public static final String ACK      = "ack";

    /**
     * Error message used if an IOException arises while parsing the
     * InputStream of a socket during deserialization of a KVMessage.
     */
    public static final String ERROR_COULD_NOT_RECEIVE_DATA =
        "Network Error: Could not receive data";

    /**
     * Error message used if an IOException arises while attempting to send
     * data through a socket.
     */
    public static final String ERROR_COULD_NOT_SEND_DATA =
        "Network Error: Could not send data";

    /**
     * Error message used if unable to create a socket.
     */
    public static final String ERROR_COULD_NOT_CREATE_SOCKET =
        "Network Error: Could not create socket";

    /**
     * Error message used if an IOException arises while trying to connect
     * a socket to another endpoint.
     */
    public static final String ERROR_COULD_NOT_CONNECT =
        "Network Error: Could not connect";

    /**
     * Error message used when a SocketTimeoutException is thrown while blocked
     * on a read of the InputStream of a socket.
     */
    public static final String ERROR_SOCKET_TIMEOUT =
        "Network Error: Socket timeout";

    /**
     * Error message used if any exception arises from the usage of libraries to
     * serialize or deserialize KVMessages. This may include parsers,
     * transformers, or any other libraries used.
     */
    public static final String ERROR_PARSER =
        "XML Error: Parser Error";

    /**
     * Error message used if a KVMessage does not exist in a valid state before
     * serialization or after deserialization. For example, if a KVMessage is of
     * type putreq but has a null value field when sendMessage is called. This
     * error should only be raised when not all the necessary fields are set.
     * If extra fields are set (like value exists for a getreq), you may, but
     * are not required to, throw this error.
     */
    public static final String ERROR_INVALID_FORMAT =
        "XML Error: Message format incorrect";

    /**
     * Error message used if a GET or DEL request is made on a key that does not
     * have a value associated with it.
     */
    public static final String ERROR_NO_SUCH_KEY =
        "Data Error: Key does not exist";

    /**
     * Error message used if a PUT request is made with a key longer than 256
     * characters. This condition must be checked on the server in KVServer and
     * not anywhere else such as KVClient and KVMessage.
     */
    public static final String ERROR_OVERSIZED_KEY =
        "Data Error: Oversized key";

    /**
     * Error message used if a PUT request is made with a value longer than
     * 256*1024 characters. This condition must be checked on the server in
     * KVServer and not anywhere else such as KVClient and KVMessage.
     */
    public static final String ERROR_OVERSIZED_VALUE =
        "Data Error: Oversized value";

    /**
     * Error message used if a request is made with a key that is null or an
     * empty string.
     */
    public static final String ERROR_INVALID_KEY =
        "Data Error: Null or empty key";

    /**
     * Error message used if a request is made with a value that is null or an
     * empty string.
     */
    public static final String ERROR_INVALID_VALUE =
        "Data Error: Null or empty value";

}
