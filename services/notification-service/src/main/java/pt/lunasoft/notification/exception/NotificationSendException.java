package pt.lunasoft.notification.exception;

public class NotificationSendException extends RuntimeException {
	
    private static final long serialVersionUID = 8704309208243672363L;

	public NotificationSendException(String message) {
        super(message);
    }

    public NotificationSendException(String message, Throwable cause) {
        super(message, cause);
    }
    
}