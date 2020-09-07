package bs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.TreeMap;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

public class BaseStation extends Thread {

	public static final int TRAFFIC_ARRIVE = 3;
	public static final int TRAFFIC_EGRESS = 4;
	public static final int NEW_STATE = 5;
	public static final int CLOSE = -1;

	public static final int ON = 1;
	public static final int OFF = 2;
	public static final int TO_ON = 3;
	public static final int TO_OFF = 4;
	public static final int HISTERISIS = -1;
	public static final int WAITING_TO_ON = -2;

	private DatagramSocket sc;
	private DatagramPacket dp;

	protected double c = 1;
	protected double tToOff = 0;
	protected double tToOn = 0;
	protected double tHysterisis = 0;
	protected double algorithmParam = 1;
	protected char algorithm = Algoritmos.NO_COALESCING;

	protected double q = 0;
	protected int state;
	protected int nextState;
	protected boolean procesando = false;
	protected Tarea tareaProcesandose;
	protected TreeMap<Long, Tarea> listaTareasPendientes = new TreeMap<>();

	public BaseStation(String args[]) {
		imprimirComandos(args);
		String ipBroker = "localhost";
		int puertoBroker = 3000;
		int id = Integer.parseInt(args[0]);
		double x = Double.parseDouble(args[1]);
		double y = Double.parseDouble(args[2]);

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--host")) {
				ipBroker = args[++i];
				puertoBroker = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-a") || args[i].equals("--algorithm")) {
				algorithm = args[++i].charAt(0);
				algorithmParam = Double.parseDouble(args[++i]);
			} else if (args[i].equals("-t") || args[i].equals("--times")) {
				tToOff = Double.parseDouble(args[++i]);
				tToOn = Double.parseDouble(args[++i]);
				tHysterisis = Double.parseDouble(args[++i]);
			} else if (args[i].equals("-c") || args[i].equals("--capacity"))
				c = Double.parseDouble(args[++i]);
		}

		System.out.println("BaseStation iniciada con los parámetros:\n\tid=" + id + " x=" + x + " y=" + y + " host="
				+ ipBroker + " port=" + puertoBroker + " capacity=" + c + "\n\ttToOff=" + tToOff + " tToOn=" + tToOn
				+ " tHysteresis=" + tHysterisis + " algorithm=" + algorithm + " algorithmParam=" + algorithmParam);

		registrar(ipBroker, puertoBroker, id, x, y);
	}

	private void registrar(String ipBroker, int puertoBroker, int id, double x, double y) {
		MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
		try {
			sc = new DatagramSocket();
			packer.packInt(2).packInt(id).packDouble(x).packDouble(y).close();
			byte[] mensaje = packer.toByteArray();
			InetAddress ad = InetAddress.getByName(ipBroker);
			dp = new DatagramPacket(mensaje, mensaje.length, ad, puertoBroker);
			sc.send(dp);
		} catch (IOException ex) {
			System.out.println("Error al realizar el registro. Ejecución finalizada");
			System.exit(-1);
		}

	}

	public static void main(String[] args) {
		new BaseStation(args).run();
	}

	@Override
	public void run() {

		while (true) {
			MessageUnpacker mensaje = recibirMensaje();

			try {

				switch (mensaje.unpackInt()) {
				case TRAFFIC_ARRIVE:
					procesarTrafficArrival(mensaje);
					break;
				case TRAFFIC_EGRESS:
					procesarTrafficEgress(mensaje);
					break;
				case NEW_STATE:
					procesarNewState(mensaje);
					break;
				case CLOSE:
					sc.close();
					return;
				}

			} catch (Exception e) {
				System.out.println("Error al intentar empaquetar/desempaquetar un mensaje. Ejecución finalizada");
				System.exit(-1);
			}

		}

	}

	private MessageUnpacker recibirMensaje() {
		MessageUnpacker unpacker = null;
		try {
			byte[] data = new byte[50];
			sc.receive(new DatagramPacket(data, data.length));
			unpacker = MessagePack.newDefaultUnpacker(data);
		} catch (IOException ex) {
			System.out.println("Error al intentar recibir un mensaje. Ejecución finalizada");
			System.exit(-1);
		}

		return unpacker;
	}

	private void enviarMensaje(MessageBufferPacker packer) {
		try {
			byte[] mensaje = packer.toByteArray();
			dp.setData(mensaje, 0, mensaje.length);
			sc.send(dp);
		} catch (IOException ex) {
			System.out.println("Error al intentar enviar un mensaje. Ejecución finalizada");
			System.exit(-1);
		}
	}

	public void procesarTrafficArrival(MessageUnpacker request) throws IOException {

		double t = request.unpackDouble();
		long id = request.unpackLong();
		double size = request.unpackDouble();
		request.close();

		Tarea tarea = new Tarea(id, size, t);
		listaTareasPendientes.put(id, tarea);
		q += size;

		double tNewState = Algoritmos.algoritmoActivacion(this, false);
		double tTrafficEgress = Algoritmos.algoritmoProcesado(this);
		double a = Tarea.getDelay(t);

		MessageBufferPacker response = MessagePack.newDefaultBufferPacker();
		response.packDouble(q).packInt(state).packDouble(tTrafficEgress).packDouble(tNewState).packInt(nextState)
				.packDouble(a).close();
		enviarMensaje(response);
	}

	public void procesarTrafficEgress(MessageUnpacker request) throws IOException {

		double t = request.unpackDouble();
		request.close();
		long id = tareaProcesandose.getId();
		double size = tareaProcesandose.getSize();
		double w = t - tareaProcesandose.gettArrive() - tareaProcesandose.getSize() / c;

		procesando = false;

		double tNewState = Algoritmos.algoritmoSuspension(this);
		double tTrafficEgress = Algoritmos.algoritmoProcesado(this);

		MessageBufferPacker response = MessagePack.newDefaultBufferPacker();
		response.packDouble(q).packInt(state).packDouble(tTrafficEgress).packDouble(tNewState).packInt(nextState)
				.packDouble(w).packLong(id).packDouble(size).close();
		enviarMensaje(response);
	}

	public void procesarNewState(MessageUnpacker request) throws IOException {

		int estadoRecibido = request.unpackInt();
		request.close();

		double tNewState = 0;

		state = estadoRecibido;

		switch (state) {
		case TO_OFF:
			nextState = OFF;
			tNewState = tToOff;
			break;
		case TO_ON:
			nextState = ON;
			tNewState = tToOn;
			break;
		case OFF:
			tNewState = Algoritmos.algoritmoActivacion(this, true);
			break;
		}

		double tTrafficEgress = Algoritmos.algoritmoProcesado(this);

		MessageBufferPacker response = MessagePack.newDefaultBufferPacker();
		response.packDouble(q).packInt(state).packDouble(tTrafficEgress).packDouble(tNewState).packInt(nextState)
				.close();
		enviarMensaje(response);
	}

	private static void imprimirComandos(String args[]) {

		for (int i = 0; i < args.length; i++)
			if (args[i].equals("-h") || args[i].equals("--help")) {
				System.out.println("Argumentos obligatorios:\n"
						+ "\t<ID> <X> <Y>\n"
						+ "Argumentos opcionales:\n"
						+ "\t[--host <IP> <PUERTO>] [-c|--capacity <CAPACIDAD>] [-t|--times <TTOOFF> <TTOON> <THISTERISIS>]\n"
						+ "\t[-a|--algorithm <ALGORITMO> <PARAM>] [-h|--help]\n"
						+ "Información sobre los argumentos:\n"
						+ "\t<ID>\t\tDefine el ID con el cual se identifica la entidad BS.\n"
						+ "\t<X>\t\tDefine la coordenada X inicial donde se sitúa la entidad BS.\n"
						+ "\t<Y>\t\tDefine la coordenada Y inicial donde se sitúa la entidad BS.\n"
						+ "\t--host <IP> <PUERTO>\n"
						+ "\t\t\tDefine la IP o el nombre del host donde está ubicado el broker y su puerto.\n"
						+ "\t\t\tValor por defecto: localhost 3000.\n"
						+ "\t-c|--capacity <CAPACIDAD>\n"
						+ "\t\t\tDefine la velocidad de procesamiento de tareas.\n"
						+ "\t\t\tValor por defecto: 1.\n"
						+ "\t-t|--times <TTOOFF> <TTOON> <THISTERISIS>\n"
						+ "\t\t\tDefine el tiempo de desactivación, activación e histéresis respectivamente.\n"
						+ "\t\t\tValor por defecto: 0 0 0.\n"
						+ "\t-a|--algorithm <ALGORITMO>  <PARAM>\n"
						+ "\t\t\tDefine el comportamiento del algoritmo de decisión de estado. Valores permitidos para ALGORITMO:\n"
						+ "\t\t\t\tn[o coalescing]\n"
						+ "\t\t\t\ts[ize based coalescing] (umbralON=PARAM)\n"
						+ "\t\t\t\tt[ime based coalescing] (temporizador=PARAM)\n"
						+ "\t\t\t\tf[ixed coalescing]\t(periodo=PARAM)\n"
						+ "\t\t\tValor por defecto: n 1.\n"
						+ "\t-h|--help\tImprime por consola la lista de posibles argumentos con su explicación.\n");
				System.exit(0);
			}
	}
}
