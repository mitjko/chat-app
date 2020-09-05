package chat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientApp extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	private JTextField tf;
	private JTextField tfServer, tfPort, tfUsername;
	private JButton b1, b2;
	private JTextArea ta;
	private boolean connected;
	private Client client;
	private int defaultPort;
	private String defaultHost;
	private SimpleDateFormat sdf;

	ClientApp(String host, int port) {

		super("Chat Client");
		defaultPort = port;
		defaultHost = host;
		sdf = new SimpleDateFormat("HH:mm:ss");
		// north
		JPanel northPanel = new JPanel(new GridLayout(1, 1));
		JPanel serverAndPort = new JPanel(new GridLayout(1, 3, 30, 0));
		Box boxes[] = new Box[3];
		boxes[0] = Box.createHorizontalBox();
		boxes[1] = Box.createHorizontalBox();
		boxes[2] = Box.createHorizontalBox();

		tfServer = new JTextField(host);
		tfPort = new JTextField("" + port);
		tfUsername = new JTextField("Guest");
		boxes[0].add(new JLabel("Username:"));
		boxes[0].add(Box.createHorizontalStrut(10));
		boxes[0].add(tfUsername);
		boxes[1].add(new JLabel("IP:"));
		boxes[1].add(Box.createHorizontalStrut(10));
		boxes[1].add(tfServer);
		boxes[2].add(new JLabel("Port Number:"));
		boxes[2].add(Box.createHorizontalStrut(10));
		boxes[2].add(tfPort);

		serverAndPort.add(boxes[0]);
		serverAndPort.add(boxes[1]);
		serverAndPort.add(boxes[2]);

		northPanel.add(serverAndPort);
		add(northPanel, BorderLayout.NORTH);

		// center
		JPanel centerPanel = new JPanel(new GridLayout(1, 1));
		ta = new JTextArea("Welcome to the Chat room\n", 80, 80);
		centerPanel.add(new JScrollPane(ta));
		ta.setEditable(false);

		add(centerPanel, BorderLayout.CENTER);

		JPanel southPanel = new JPanel(new GridLayout(2, 1));

		JPanel s1 = new JPanel(new GridLayout(2, 1));
		JPanel s2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));

		b1 = new JButton("Connect");
		b1.setPreferredSize(new Dimension(100, 40));
		b1.addActionListener(this);
		b2 = new JButton("Disconnect");
		b2.setPreferredSize(new Dimension(100, 40));
		b2.addActionListener(this);
		b2.setEnabled(false);
		tf = new JTextField("");
		tf.setBackground(Color.WHITE);
		tf.setEditable(false);
		s1.add(new JLabel("ENTER YOUR MESSAGE BELOW", SwingConstants.CENTER));
		s1.add(tf);

		s2.add(b1);
		s2.add(b2);
		southPanel.add(s1);
		southPanel.add(s2);
		add(southPanel, BorderLayout.SOUTH);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setResizable(false);
		setSize(600, 600);
		setVisible(true);
		tf.requestFocus();

	}

	void append(String str) {
		ta.append(str);
		ta.setCaretPosition(ta.getText().length() - 1);
	}

	void connectionFailed() {
		b1.setEnabled(true);
		b2.setEnabled(false);
		tfUsername.setText("Guest");
		tfPort.setText("" + defaultPort);
		tfServer.setText(defaultHost);
		tf.setText(null);
		tf.setEditable(false);
		tfUsername.setEditable(true);
		tfServer.setEditable(true);
		tfPort.setEditable(true);
		tf.removeActionListener(this);
		connected = false;
	}

	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		// DC button
		if (o == b2) {
			client.sendMessage(new Message(Message.LOGOUT, ""));
			return;
		}

		// connected already
		if (connected) {
			String msg = tf.getText();
			if (msg.startsWith("/msg")) {
				String[] msgArray = msg.split(" ", 3);
				if (msgArray.length != 3) {
					append("Invalid command . . .\n");
					tf.setText("");
					return;
				}
				msg = msgArray[1] + " " + msgArray[2];
				String time = sdf.format(new Date()) + " " + "to " + msgArray[1] + ": " + msgArray[2] + "\n";
				append(time);
				client.sendMessage(new Message(Message.PRIVATE, msg));
			} else if (msg.equals("/online")) {
				client.sendMessage(new Message(Message.ONLINE, ""));
			} else {
				if (msg.length() == 0)
					return;
				client.sendMessage(new Message(Message.MESSAGE, tf.getText()));
			}
			tf.setText("");
			return;
		}

		// Connect button - try to connect
		if (o == b1) {
			String username = tfUsername.getText().trim();
			if (username.length() == 0)
				return;
			String server = tfServer.getText().trim();
			if (server.length() == 0)
				return;
			String portNumber = tfPort.getText().trim();
			if (portNumber.length() == 0)
				return;
			int port = 0;
			try {
				port = Integer.parseInt(portNumber);
			} catch (Exception en) {
				return;
			}

			client = new Client(server, port, username, this);

			if (!client.start())
				return;
			ta.setText(null);
			tf.setText(null);
			append("You have successfully connected to the server!\n");
			connected = true;

			b1.setEnabled(false);
			b2.setEnabled(true);
			tfUsername.setEditable(false);
			tfServer.setEditable(false);
			tfPort.setEditable(false);
			tf.setEditable(true);
			tf.addActionListener(this);
		}

	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new ClientApp("localhost", 1234);
			}
		});
	}

}
