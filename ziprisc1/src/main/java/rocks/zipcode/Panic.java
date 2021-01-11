package rocks.zipcode;

// this class is a general exception (a dogsbody) to be used throughout this project
// to indicate an error somewhere.

public class Panic extends Error {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public Panic() {
        super();
    }
    public Panic(String message) { super(message); }
    public Panic(String message, Throwable cause) { super(message, cause); }
    public Panic(Throwable cause) { super(cause); }  
}

