package chat;

import java.io.Serializable;

/*
 * This class defines the different type of messages that will be exchanged between the
 * Clients and the Server. 
*/
public class Message implements Serializable {

	protected static final long serialVersionUID = 1112122200L;

	// The different types of message sent by the Client
	// ONLINE to receive the list of the users connected
	// MESSAGE an ordinary message
	// LOGOUT to disconnect from the Server
	// PRIVATE to message a single client
	static final int ONLINE = 0, MESSAGE = 1, PRIVATE = 2, LOGOUT = 3;
	private int type;
	private String message;

	Message(int type, String message) {
		this.type = type;
		this.message = message;
	}

	int getType() {
		return type;
	}

	String getMessage() {
		return message;
	}
}
