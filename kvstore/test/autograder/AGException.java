package autograder;

public class AGException extends java.lang.AssertionError {

    private static final long serialVersionUID = 1L;

    AGException.ErrCat errcat;

    public AGException(AGException.ErrCat ec, String message, Throwable cause) {
        super(message);
        initCause(cause);
        errcat = ec;
    }

    public AGException(String message, Throwable cause) {
        this(ErrCat.GENERAL, message, cause);
    }

    @Override
    public String toString() {
        return errcat.friendlyName + ": " + getMessage();
    }

    public static enum ErrCat {
        GENERAL("EXCEPTION"),
        PREREQ("ERROR IN PREREQUISITE"),
        EXPECTED("WRONG EXCEPTION THROWN"),
        INTERNAL("INTERNAL AUTOGRADER ERROR");

        public String friendlyName;

        ErrCat(String fn) {
            friendlyName = fn;
        }
    }
}
