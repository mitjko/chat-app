package chat;

import java.net.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import javax.swing.SwingUtilities;

public class Client {

	private ObjectInputStream sInput;
	private ObjectOutputStream sOutput;
	private Socket socket;
	private String server, username;
	private int port;
	private ClientApp app;

	Client(String server, int port, String username) {
		this(server, port, username, null);
	}

	Client(String server, int port, String username, ClientApp app) {
		this.server = server;
		this.port = port;
		this.username = username;
		this.app = app;
	}

	public boolean start() {

		try {
			socket = new Socket(server, port);
		} catch (Exception e) {
			display("Error connecting to server:" + e);
			return false;
		}

		// String msg = "Connection accepted " +
		// socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
		// display(msg);

		try {
			sInput = new ObjectInputStream(socket.getInputStream());
			sOutput = new ObjectOutputStream(socket.getOutputStream());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			display("Exception creating new Input/output Streams: ");
			e1.printStackTrace();
			return false;
		}

		try {
			sOutput.writeObject(username);
			String answer = (String) sInput.readObject();

			if (answer.equals("NAMEALREADYEXISTS")) {
				display("Name already taken . . .");
				return false;
			}
			display("Connection accepted " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
		} catch (IOException eIO) {
			display("Exception doing login : " + eIO);
			disconnect();
			return false;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		// creates the Thread to listen from the server
		new ListenFromServer().start();
		return true;
	}

	void sendMessage(Message msg) {
		try {
			sOutput.writeObject(msg);
		} catch (IOException e) {
			System.exit(0);
			// display("Exception writing to server: " + e);
		}
	}

	private void disconnect() {
		try {
			if (sOutput != null)
				sOutput.close();
			if (socket != null)
				socket.close();
			if (sInput != null)
				sInput.close();
		} catch (IOException e) {
			// e.printStackTrace();
		}
		if (app != null)
			app.connectionFailed();
	}

	private void display(String msg) {
		if (app == null)
			System.out.println(msg); // print in console mode
		else {
			app.append(msg + "\n"); // append to the ClientGUI
		}
	}

	public static void main(String[] args) {
		// default values
		int portNumber = 1234;
		String serverAddress = "localhost";
		String userName = "Guest#" + (int) (Math.random() * 32543 % 10000);
		switch (args.length) {
		// > javac Client username portNumber serverAddr
		case 3:
			serverAddress = args[2];
			// > javac Client username portNumber
		case 2:
			try {
				portNumber = Integer.parseInt(args[1]);
			} catch (Exception e) {
				System.out.println("Invalid port number.");
				System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
				return;
			}
			// > javac Client username
		case 1:
			userName = args[0];
			// > java Client
		case 0:
			break;
		}

		Client client = new Client(serverAddress, portNumber, userName);
		// test if we can start the connection to the Server
		if (!client.start())
			return;

		Scanner scan = new Scanner(System.in);
		while (true) {
			System.out.print("> ");
			String msg = scan.nextLine();
			// message logout
			if (msg.equalsIgnoreCase("/logout")) {
				client.sendMessage(new Message(Message.LOGOUT, ""));
				break;
				// private message
			} else if (msg.startsWith("/msg")) {
				String[] msgArray = msg.split(" ", 3);
				if (msgArray.length != 3) {
					System.out.println("Invalid command . . .");
					continue;
				}
				msg = msgArray[1] + " " + msgArray[2];
				client.sendMessage(new Message(Message.PRIVATE, msg));
			}
			// message online
			else if (msg.equalsIgnoreCase("/online")) {
				client.sendMessage(new Message(Message.ONLINE, ""));
			} else { // default to ordinary message
				client.sendMessage(new Message(Message.MESSAGE, msg));
			}
		}
		// close everything related to client
		client.disconnect();
	}

	class ListenFromServer extends Thread {
		public void run() {
			while (true) {
				try {
					String msg = (String) sInput.readObject();
					if (app == null) {
						System.out.println(msg);
						System.out.print("> ");
					} else {
						SwingUtilities.invokeAndWait(new Runnable() {

							@Override
							public void run() {
								app.append(msg);
							}
						});
					}
				} catch (IOException e) {
					// display("You have been disconnected.\n");
					if (app != null)
						app.connectionFailed();
					break;
				} catch (ClassNotFoundException e2) {
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
