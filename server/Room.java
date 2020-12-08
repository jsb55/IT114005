package server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Room implements AutoCloseable {
	private static SocketServer server;// used to refer to accessible server functions
	private String name;
	private final static Logger log = Logger.getLogger(Room.class.getName());

	// Commands
	private final static String COMMAND_TRIGGER = "/";
	private final static String CREATE_ROOM = "createroom";
	private final static String JOIN_ROOM = "joinroom";
	private final static String FLIP = "flip";
	private final static String ROLL = "roll";
	private final static String MUTE = "mute";
	private final static String UNMUTE = "unmute";
	private final static String PRIVATE = "@";

	public Room(String name) {
		this.name = name;
	}

	public static void setServer(SocketServer server) {
		Room.server = server;
	}

	public String getName() {
		return name;
	}

	private List<ServerThread> clients = new ArrayList<ServerThread>();

	protected synchronized void addClient(ServerThread client) {
		client.setCurrentRoom(this);
		if (clients.indexOf(client) > -1) {
			log.log(Level.INFO, "Attempting to add a client that already exists");
		} else {
			clients.add(client);
			if (client.getClientName() != null) {
				client.sendClearList();
				sendConnectionStatus(client, true, "joined the room " + getName());
				updateClientList(client);
			}
		}
	}

	private void updateClientList(ServerThread client) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread c = iter.next();
			if (c != client) {
				boolean messageSent = client.sendConnectionStatus(c.getClientName(), true, null);
			}
		}
	}

	protected synchronized void removeClient(ServerThread client) {
		clients.remove(client);
		if (clients.size() > 0) {
			// sendMessage(client, "left the room");
			sendConnectionStatus(client, false, "left the room " + getName());
		} else {
			cleanupEmptyRoom();
		}
	}

	private void cleanupEmptyRoom() {
		// If name is null it's already been closed. And don't close the Lobby
		if (name == null || name.equalsIgnoreCase(SocketServer.LOBBY)) {
			return;
		}
		try {
			log.log(Level.INFO, "Closing empty room: " + name);
			close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void joinRoom(String room, ServerThread client) {
		server.joinRoom(room, client);
	}

	protected void joinLobby(ServerThread client) {
		server.joinLobby(client);
	}

	/***
	 * Helper function to process messages to trigger different functionality.
	 * 
	 * @param message The original message being sent
	 * @param client  The sender of the message (since they'll be the ones
	 *                triggering the actions)
	 */
	protected String processCommands(String message, ServerThread client) {
		String response = null;
		try {
			if (message.indexOf(COMMAND_TRIGGER) > -1) {
				String[] comm = message.split(COMMAND_TRIGGER);
				log.log(Level.INFO, message);
				String part1 = comm[1];
				String[] comm2 = part1.split(" ");
				String command = comm2[0];
				if (command != null) {
					command = command.toLowerCase();
				}
				String roomName;
				switch (command) {
				case CREATE_ROOM:
					roomName = comm2[1];
					if (server.createNewRoom(roomName)) {
						joinRoom(roomName, client);
					}
					break;
				case JOIN_ROOM:
					roomName = comm2[1];
					joinRoom(roomName, client);
					break;
				case FLIP:
					String coin = "";
					if (Math.random() < 0.5) {
						coin = "<b style=color:green> Flipped heads</b>";
					} else {
						coin = "<b style=color:purple> Flipped tails</b>";
					}
					response = coin;
					break;
				case ROLL:
					String[] dice = new String[] { "1", "2", "3", "4", "5", "6" };
					Random random = new Random();
					int index = random.nextInt(dice.length);
					String toString = Integer.toString(index);
					String rollResult = "<b style=color:red> Rolled " + toString + "</b>";
					response = rollResult;
				case MUTE:
					String[] muteArray = comm2[1].split(PRIVATE);
					String muteUser = muteArray[1];
					if (!client.clientMuteList.contains(muteUser)) {
						client.clientMuteList.add(muteUser);
						clientMute(client, muteUser);
						break;
					}
				case UNMUTE:
					String[] unMuteArray = comm2[1].split(PRIVATE);
					String unMuteUser = unMuteArray[1];
					if (client.clientMuteList.contains(unMuteUser)) {
						client.clientMuteList.remove(unMuteUser);
						clientUnmute(client, unMuteUser);
					}
					break;

				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	// TODO changed from string to ServerThread
	protected void sendConnectionStatus(ServerThread client, boolean isConnect, String message) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread c = iter.next();
			boolean messageSent = c.sendConnectionStatus(client.getClientName(), isConnect, message);
			if (!messageSent) {
				iter.remove();
				log.log(Level.INFO, "Removed client " + c.getId());
			}
		}
	}

	protected void clientMute(ServerThread client, String reciever) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread a = iter.next();

			if (a.getClientName().equals(reciever)) {
				a.send(client.getClientName(),
						" <b style=color:red><i>has muted you. The user will be unable to see your messages.</i></b>");
			}
		}
	}

	protected void clientUnmute(ServerThread client, String reciever) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread a = iter.next();

			if (a.getClientName().equals(reciever)) {
				a.send(client.getClientName(),
						" <b style=color:red><i>has unmuted you. The user will now be able to see your messages.</i></b>");
			}
		}
	}

	/***
	 * Takes a sender and a message and broadcasts the message to all clients in
	 * this room. Client is mostly passed for command purposes but we can also use
	 * it to extract other client info.
	 * 
	 * @param sender  The client sending the message
	 * @param message The message to broadcast inside the room
	 */
	protected void sendMessage(ServerThread sender, String message) {
		log.log(Level.INFO, getName() + ": Sending message to " + clients.size() + " clients");
		String response = processCommands(message, sender);
		if (response != null) {
			message = response;
		}

		boolean privateMessage = false;
		String recipient = "";
		try {
			if (message.startsWith("@")) {
				int pm = message.indexOf(" ");
				privateMessage = true;
				recipient = message.substring(1, pm);
				message = "<b style=color:blue> <u>*Private*</b></u> " + message.substring(pm);
			}
		} catch (Exception d) {

			log.log(Level.INFO, "Input invalid");

			return;
		}

		Iterator<ServerThread> iter = clients.iterator();

		while (iter.hasNext()) {
			ServerThread client = iter.next();
			if (!client.isMuted(sender.getClientName()) && !privateMessage || client.getClientName().equals(recipient)
					|| sender.getClientName().equals(client.getClientName())) {
				boolean messageSent = client.send(sender.getClientName(), message);
				if (!messageSent) {
					iter.remove();
					log.log(Level.INFO, "Removed client" + client.getId());
				}
			}
		}
	}

	public List<String> getRooms() {
		return server.getRooms();
	}

	@Override
	public void close() throws Exception {
		int clientCount = clients.size();
		if (clientCount > 0) {
			log.log(Level.INFO, "Migrating " + clients.size() + " to Lobby");
			Iterator<ServerThread> iter = clients.iterator();
			Room lobby = server.getLobby();
			while (iter.hasNext()) {
				ServerThread client = iter.next();
				lobby.addClient(client);
				iter.remove();
			}
			log.log(Level.INFO, "Done Migrating " + clients.size() + " to Lobby");
		}
		server.cleanupRoom(this);
		name = null;
		// should be eligible for garbage collection now
	}

}