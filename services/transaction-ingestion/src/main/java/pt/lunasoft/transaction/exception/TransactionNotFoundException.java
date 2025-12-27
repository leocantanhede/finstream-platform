package pt.lunasoft.transaction.exception;

public class TransactionNotFoundException extends RuntimeException {
	
    private static final long serialVersionUID = -186343228481847205L;

	public TransactionNotFoundException(String message) {
        super(message);
    }
	
}