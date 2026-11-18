/** User-defined exception for invalid input or rule violations. */
public class ValidationException extends Exception {
    public ValidationException(String message) {
        super(message);
    }
}
