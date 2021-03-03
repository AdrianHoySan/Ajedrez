package chess;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.TreeMap;

import entrada.Coordenada;
import entrada.Herramientas;

import modelo.Color;
import modelo.Player;
import sockets.Message;

public class ChessClient {

	private String ip;
	private int port;
	private Socket socket;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	private Player player;

	public static void main(String[] args) {

		ChessClient cc = new ChessClient();
		cc.run();

	}

	private void run() {

		int option;

		do {
			showStartMenu();
			option = Herramientas.pedirInt("Enter option (0-Exit):");
			switch (option) {
			case 1:
				connect();
				if (socket != null) {
					getPlayer();
					createGame();
				}
				break;
			case 2:
				connect();
				if (socket != null) {
					getPlayer();
					addToGame();
				}
				break;
			default:
				showStartMenu();
				System.out.println("Option not valid. Enter option (0-Exit):");
				break;
			}
		} while (option != 0);

	}

	private void addToGame() {
		
		Message mOut, mIn;

		int game;

		mOut = new Message(Message.Type.GET_CREATED_GAMES,
				"Requesting the list of games waiting for the player " + player.getName());
		mOut.setPlayer(player);

		mIn = sendMessageAndWaitResponse(mOut);
		TreeMap<Integer, String[]> listadoTotal = mIn.getListOfGames();
		TreeMap<Integer, String[]> listadoPosible = new TreeMap<Integer, String[]>();

		for (Integer key : listadoTotal.keySet()) {
			if (player.getColor() == Color.WHITE && listadoTotal.get(key)[0] == null
					|| player.getColor() == Color.BLACK && listadoTotal.get(key)[1] == null)
				listadoPosible.put(key, listadoTotal.get(key));
		}
		
		if (listadoPosible.isEmpty()) {
			System.out.println("No hay partidas en espera para ese color.");
		} else {

			do {
				imprimirPartidas(listadoPosible);
				game = Herramientas.pedirInt("Select the id of the desired game");
				if (!mIn.getListOfGames().keySet().contains(game))
					System.out.println("The game does not exist.");

			} while (!mIn.getListOfGames().keySet().contains(game) && game != 0);

			mOut = new Message(Message.Type.ADD_TO_GAME,
					"Requesting to add the player " + player.getName() + " to the game " + game);
			mOut.setGameId(game);
			mOut.setPlayer(player);

			mIn = sendMessageAndWaitResponse(mOut);
			if (mIn.getMessageType() == Message.Type.ADDED_TO_GAME)
				play();
		}
	}


	private void createGame() {

		Message mIn, mOut;
		mOut = new Message(Message.Type.CREATE_GAME, "Crear un nuevo juego.");
		mOut.setPlayer(player);

		mIn = sendMessageAndWaitResponse(mOut);

		if (mIn.getMessageType() == Message.Type.GAME_CREATED_WAITING) {
			System.out.println(mIn.getDescription());
			play();
		} else {
			showStartMenu();
			System.out.println("The game could not be created.");
		}

	}

	private void play() {

		try {
		
			Message mOut = null, mIn = null;
			boolean exit = false;
			Coordenada c;
			
			player.setOis(ois);
			player.setOos(oos);
			while(exit == false) {
				mIn = (Message)player.getOis().readObject();
				if(mIn.getMessageType() == Message.Type.EXIT) {
					exit = true;
				} else if (mIn.getMessageType() == Message.Type.GAME_INFORMATION) {
					System.out.println(mIn.getDescription());
				} else if (mIn.getMessageType() == Message.Type.COORDINATE_REQUEST) {
					mOut = new Message(Message.Type.SEND_COORDINATE,Herramientas.pedirString("Introduce la coordenada"));
					player.getOos().writeObject(mOut);
				}
				
			}
			// To do
			// While you don't have to exit and the message read is not null
			// If the message is to show information, you only have to show it,
			// but if the message request a coordinate, your have to provide it.
			

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void connect() {

		ip = Herramientas.pedirString("Introduce la direccion IP: ");
		port = Herramientas.pedirInt("Introduce el puerto:");

		try {

			this.socket = new Socket(ip, port);

			this.ois = new ObjectInputStream(socket.getInputStream());
			this.oos = new ObjectOutputStream(socket.getOutputStream());

			System.out.println("Conexion establecida correctamente!!");

		} catch (Exception e) {
			System.out.println("No se ha podido realizar la conexion.");
		}

	}

	private void getPlayer() {

		String nombre = Herramientas.pedirString("Dame tu nombre");

		String c = Herramientas.pedirString("Dime tu color [w|b]:").toLowerCase().substring(0, 1);
		try {
			if (c.equals("b"))
				player = new Player(nombre, Color.BLACK, socket.getLocalAddress());
			else
				player = new Player(nombre, Color.WHITE, socket.getLocalAddress());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private Message sendMessageAndWaitResponse(Message mOut) {

		Message mIn = null;

		try {

			oos.writeObject(mOut);
			mIn = (Message) ois.readObject();

		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}

		return mIn;
	}

	private void showStartMenu() {

		update();
		System.out.println(" ╔════════════════════════════╗");
		System.out.println(" ║            Menu            ║");
		System.out.println(" ╟────────────────────────────╢");
		System.out.println(" ║      1- Create game        ║");
		System.out.println(" ║      2- Add to game        ║");
		System.out.println(" ╟────────────────────────────╢");
		System.out.println(" ║      0- Exit               ║");
		System.out.println(" ╚════════════════════════════╝");

	}

	private void imprimirPartidas(TreeMap<Integer, String[]> listado) {

		update();
		
		for(Integer id : listado.keySet()) {
			
			if(listado.get(id)[0] == null)
				System.out.println(id + " " + listado.get(id)[1]);
			else
				System.out.println(id + " " + listado.get(id)[0]);
			
		}
		// To do
		// Show the list of games
		

	}

	private void update() {
		System.out.flush();
	}

}
