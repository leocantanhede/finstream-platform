package pt.lunasoft.notification.exception;

public class NotificationNotFoundException extends RuntimeException {
	
    private static final long serialVersionUID = -5107769999441916090L;

	public NotificationNotFoundException(String message) {
        super(message);
    }
	
}