package chat;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {
	// an ArrayList to keep the list of the Client
	private ArrayList<ClientThread> al;
	// to display time
	private SimpleDateFormat sdf;
	// the port number to listen for connection
	private int port;
	// the boolean that will be set to false to stop the server
	private boolean running;

	/*
	 * server constructor that receive the port to listen to for connection as
	 * parameter in console
	 */

	public Server(int port) {
		this.port = port;
		sdf = new SimpleDateFormat("HH:mm:ss");
		al = new ArrayList<ClientThread>();
	}

	public void start() {
		running = true;
		// create socket server and wait for connection requests
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			new Thread(new ServerThread()).start();
			display("Server waiting for Clients on port " + port + ".");
			while (running) {
				Socket socket = serverSocket.accept(); // accept connection

				if (!running)
					continue;

				ClientThread ct = new ClientThread(socket);
				Thread t = new Thread(ct);
				al.add(ct);
				t.start();
			}

			try {
				serverSocket.close();
				for (ClientThread clientThread : al) {
					clientThread.running = false;
					clientThread.sInput.close();
					clientThread.sOutput.close();
					clientThread.socket.close();
				}

			} catch (Exception e) {
				display("Exception closing the server and clients: " + e);
			}
		} catch (IOException e) {
			String msg = sdf.format(new Date()) + " Exception on new ServerSocket: " + e + "\n";
			display(msg);
		}
	}

	private void stop() {
		running = false;
		// force quit
		try {
			new Socket("localhost", port);
		} catch (Exception e) {
		}
	}

	private void display(String msg) {
		String time = sdf.format(new Date()) + " " + msg;
		System.out.println(time);

	}

	private synchronized void remove(ClientThread ct) {
		al.remove(ct);
	}

	/*
	 * to broadcast a message to all Clients
	 */
	private synchronized void broadcast(String message) {
		String time = sdf.format(new Date());
		String msg = time + " " + message + "\n";
		System.out.print(msg);
		for (ClientThread clientThread : al) {
			if (!clientThread.writeMsg(msg)) {
				remove(clientThread);
				display("Disconnected Client " + clientThread.username + " removed from list.");
			}

		}
	}

	public static void main(String[] args) {
		// start server on port 1234 unless a portNumber is specified
		int portNumber = 1234;
		switch (args.length) {
		case 1:
			try {
				portNumber = Integer.parseInt(args[0]);
			} catch (Exception e) {
				System.out.println("Invalid port number.");
				System.out.println("Usage is: > java Server [portNumber]");
				return;
			}
		case 0:
			break;
		}

		Server server = new Server(portNumber);
		server.start();
	}

	/* One instance of this thread will run for each client */
	class ClientThread implements Runnable {
		private Socket socket;
		private ObjectInputStream sInput;
		private ObjectOutputStream sOutput;
		private String username;
		private Message cm;
		private String date;
		private boolean running;
		private SimpleDateFormat sdf;

		ClientThread(Socket socket) {
			running = true;
			this.socket = socket;
			try {
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput = new ObjectInputStream(socket.getInputStream());
				sdf = new SimpleDateFormat("HH:mm:ss");

				String name = (String) sInput.readObject();
				if (exists(name)) {
					writeMsg("NAMEALREADYEXISTS");
					running = false;
					return;
				}
				writeMsg("NAMEACCEPTED");
				username = name;
			} catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			date = new Date().toString() + "\n";
			broadcast(username + " has connected.");
		}

		public void run() {
			while (running) {
				try {
					cm = (Message) sInput.readObject();
				} catch (IOException e) {
					break;
				} catch (ClassNotFoundException e) {
					break;
				}

				String message = cm.getMessage();
				switch (cm.getType()) {

				case Message.MESSAGE:
					broadcast(username + ": " + message);
					break;
				case Message.PRIVATE:
					String[] msgArray = message.split(" ", 2);
					String receiver = msgArray[0];
					String msg = msgArray[1];
					send("from " + username + ": " + msg, receiver);
					break;
				case Message.LOGOUT:
					broadcast(username + " has disconnected.");
					// display(username + " disconnected with a LOGOUT
					// message.");
					running = false;
					break;
				case Message.ONLINE:
					writeMsg("List of the users connected at " + sdf.format(new Date()) + "\n");
					for (int i = 0; i < al.size(); ++i) {
						ClientThread ct = al.get(i);
						writeMsg((i + 1) + ") " + ct.username + " since " + ct.date);
					}
					break;
				}
			}
			// remove thread
			remove(this);
			close();
		}

		private synchronized void send(String msg, String receiver) {
			for (ClientThread clientThread : al) {
				if (clientThread.username.equals(receiver)) {
					String time = sdf.format(new Date()) + " " + msg + "\n";
					if (!clientThread.writeMsg(time)) {
						remove(clientThread);
						display("Disconnected Client " + clientThread.username + " removed from list.");
					}
					return;
				}
			}
			writeMsg("User is not online.\n");
		}

		private synchronized boolean exists(String username) {
			for (ClientThread clientThread : al) {
				if (clientThread.username.equals(username))
					return true;
			}
			return false;
		}

		private synchronized void close() {
			try {
				if (sOutput != null)
					sOutput.close();
			} catch (Exception e) {
			}
			try {
				if (sInput != null)
					sInput.close();
			} catch (Exception e) {
			}
			;
			try {
				if (socket != null)
					socket.close();
			} catch (Exception e) {
			}
		}

		private synchronized boolean writeMsg(String msg) {
			if (!socket.isConnected()) {
				close();
				return false;
			}
			try {
				sOutput.writeObject(msg);
			} catch (IOException e) {
				display("Error sending message to " + username);
				e.printStackTrace();
			}
			return true;
		}
	}

	public class ServerThread implements Runnable {
		// server side input
		@Override
		public void run() {
			Scanner stdIn = new Scanner(System.in);
			while (running) {
				String input = stdIn.nextLine();
				if (input.startsWith("/kick")) {
					String toKick = input.split(" ", 2)[1];
					boolean flag = false;
					for (ClientThread clientThread : al) {
						if (clientThread.username.equals(toKick)) {
							try {
								clientThread.sOutput.writeObject("You have been kicked.\n");
								clientThread.close();
								remove(clientThread);
								broadcast("User " + clientThread.username + " has been kicked . . .");
								flag = true;
								break;
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					if (!flag)
						System.out.println(toKick + " is not online . . .");
				} else if (input.equals("/quit")) {
					stop();
				}
			}
		}

	}
}
