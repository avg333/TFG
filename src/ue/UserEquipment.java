package ue;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

public class UserEquipment extends Thread {

	public static final int TRAFFIC_INGRESS = 1;
	public static final int CLOSE = -1;

	private static final char DETERMINISTA = 'd';
	private static final char UNIFORME = 'u';
	private static final char EXPONENCIAL = 'e';

	private DatagramSocket sc;
	private DatagramPacket dp;
	private Random rand = new Random();

	private double x;
	private double y;
	private char sizeDist = EXPONENCIAL;
	private char delayDist = EXPONENCIAL;
	private char mobilityDist = DETERMINISTA;
	private double sizeParam1 = 1.0;
	private double sizeParam2 = 0.0;
	private double delayParam1 = 1.0;
	private double delayParam2 = 0.0;
	private double mobilityParam1 = 0.0;
	private double mobilityParam2 = 0.0;

	private double size = -1.0;
	private double delay = 0.0;

	public UserEquipment(String[] args) {
		imprimirComandos(args);
		String ipBroker = "localhost";
		int puertoBroker = 3000;
		long semilla = 0;
		int id = Integer.parseInt(args[0]);
		x = Double.parseDouble(args[1]);
		y = Double.parseDouble(args[2]);

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--host")) {
				ipBroker = args[++i];
				puertoBroker = Integer.parseInt(args[++i]);
			} else if (args[i].equals("-s") || args[i].equals("--size")) {
				sizeDist = args[++i].charAt(0);
				sizeParam1 = Double.parseDouble(args[++i]);
				sizeParam2 = Double.parseDouble(args[++i]);
			} else if (args[i].equals("-d") || args[i].equals("--delay")) {
				delayDist = args[++i].charAt(0);
				delayParam1 = Double.parseDouble(args[++i]);
				delayParam2 = Double.parseDouble(args[++i]);
			} else if (args[i].equals("-m") || args[i].equals("--mobility")) {
				mobilityDist = args[++i].charAt(0);
				mobilityParam1 = Double.parseDouble(args[++i]);
				mobilityParam2 = Double.parseDouble(args[++i]);
			} else if (args[i].equals("--seed")) {
				semilla = Long.parseLong(args[++i]);
				rand.setSeed(semilla + id);
			}

		}

		System.out.println("UserEquipment iniciado con los parámetros:\n\tid=" + id + " x=" + x + " y=" + y + " host="
				+ ipBroker + " port=" + puertoBroker + " seed=" + semilla + "\n\tsizeDist=" + sizeDist + " sizeParam1="
				+ sizeParam1 + " sizeParam2=" + sizeParam2 + "\n\tdelayDist=" + delayDist + " delayParam1="
				+ delayParam1 + " delayParam2=" + delayParam2 + "\n\tmobilityDist=" + mobilityDist + " mobilityParam1="
				+ mobilityParam1 + " mobilityParam2=" + mobilityParam2);

		registrar(ipBroker, puertoBroker, id);
	}

	private void registrar(String ipBroker, int puertoBroker, int id) {
		MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
		try {
			sc = new DatagramSocket();
			InetAddress ad = InetAddress.getByName(ipBroker);
			packer.packInt(1).packInt(id).packDouble(x).packDouble(y).close();
			byte[] mensaje = packer.toByteArray();
			dp = new DatagramPacket(mensaje, mensaje.length, ad, puertoBroker);
			sc.send(dp);
		} catch (IOException ex) {
			System.out.println("Error al realizar el registro. Ejecución finalizada");
			System.exit(-1);
		}
	}

	public static void main(String[] args) {
		new UserEquipment(args).run();
	}

	@Override
	public void run() {

		delay = obtenerAleatorio(delayDist, delayParam1, delayParam2);

		while (true) {
			int tipo = recibirMensaje();

			switch (tipo) {
			case TRAFFIC_INGRESS:
				procesarTrafficIngress();
				break;
			case CLOSE:
				sc.close();
				return;
			}
		}

	}

	private void procesarTrafficIngress() {
		enviarTarea();
		x += obtenerAleatorio(mobilityDist, mobilityParam1, mobilityParam2);
		y += obtenerAleatorio(mobilityDist, mobilityParam1, mobilityParam2);
		size = obtenerAleatorio(sizeDist, sizeParam1, sizeParam2);
		delay = obtenerAleatorio(delayDist, delayParam1, delayParam2);
	}

	private int recibirMensaje() {
		int tipo = 0;
		try {
			byte[] data = new byte[10];
			sc.receive(new DatagramPacket(data, data.length));
			MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(data);
			tipo = unpacker.unpackInt();
			unpacker.close();
		} catch (IOException ex) {
			System.out.println("Error al intentar recibir un mensaje. Ejecución finalizada");
			System.exit(-1);
		}

		return tipo;
	}

	private void enviarTarea() {
		MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
		try {
			packer.packDouble(x).packDouble(y).packDouble(size).packDouble(delay).close();
			byte[] mensaje = packer.toByteArray();
			dp.setData(mensaje, 0, mensaje.length);
			sc.send(dp);
		} catch (IOException ex) {
			System.out.println("Error al intentar enviar un mensaje. Ejecución finalizada");
			System.exit(-1);
		}
	}

	private double obtenerAleatorio(char distribucion, double param1, double param2) {

		switch (distribucion) {
		case DETERMINISTA:
			return param1;
		case UNIFORME:
			return rand.nextDouble() * (param1 - param2) + param2;
		case EXPONENCIAL:
			if (param1 != 0)
				return Math.log(1 - rand.nextDouble()) / (-param1);
		}

		return 0;
	}

	private static void imprimirComandos(String args[]) {

		for (int i = 0; i < args.length; i++)
			if (args[i].equals("-h") || args[i].equals("--help")) {
				System.out.println("Argumentos obligatorios:\n" + "\t<ID> <X> <Y>\n" + "Argumentos opcionales:\n"
						+ "\t[--host <IP> <PUERTO>] [-s|--size <DISTR> <PARAM1> <PARAM2>] [-d|--delay <DISTR> <PARAM1> <PARAM2>]\n"
						+ "\t[-m|--mobility <DISTR> <PARAM1> <PARAM2>] [--seed <SEMILLA>] [-h|--help]\n"
						+ "Información sobre los argumentos:\n"
						+ "\t<ID>\t\tDefine el ID con el cual se identifica la entidad UE.\n"
						+ "\t<X>\t\tDefine la coordenada X inicial donde se sitúa la entidad UE.\n"
						+ "\t<Y>\t\tDefine la coordenada Y inicial donde se sitúa la entidad UE.\n"
						+ "\t--host <IP> <PUERTO>\n"
						+ "\t\t\tDefine la IP o el nombre del host donde está ubicado el broker y su puerto.\n"
						+ "\t\t\tValor por defecto: localhost 3000.\n"

						+ "\t-s|--size <DISTR> <PARAM1> <PARAM2>\n"
						+ "\t\t\tDefine cómo se obtiene el valor tiempo demandado.\n"
						+ "\t\t\tValores permitidos para DISTR:\n" + "\t\t\t\td[eterminista]\tsize=PARAM1\n"
						+ "\t\t\t\tu[niforme]\tE[size]=(PARAM1+PARAM2)/2\n"
						+ "\t\t\t\te[xponencial]\tE[size]=1/PARAM1\n" + "\t\t\tValor por defecto: e 1 0.\n"

						+ "\t-d|--delay <DISTR> <PARAM1> <PARAM2>\n"
						+ "\t\t\tDefine cómo se obtiene el valor tiempo entre llegadas.\n"
						+ "\t\t\tValores permitidos para DISTR:\n" + "\t\t\t\td[eterminista]\tdelay=PARAM1\n"
						+ "\t\t\t\tu[niforme]\tE[delay]=(PARAM1+PARAM2)/2\n"
						+ "\t\t\t\te[xponencial]\tE[delay]=1/PARAM1\n" + "\t\t\tValor por defecto: e 1 0.\n"

						+ "\t-m|--mobility <DISTR> <PARAM1> <PARAM2>\n"
						+ "\t\t\tDefine cómo se obtiene el valor del desplazamiento entre generaciones.\n"
						+ "\t\t\tValores permitidos para DISTR:\n" + "\t\t\t\td[eterminista]\tmobility=PARAM1\n"
						+ "\t\t\t\tu[niforme]\tE[mobility]=(PARAM1+PARAM2)/2\n"
						+ "\t\t\t\te[xponencial]\tE[mobility]=1/PARAM1\n" + "\t\t\tValor por defecto: d 0 0.\n"

						+ "\t--seed <SEMILLA>La generación de números aleatorios estará basada en una semilla con valor=SEMILLA+ID.\n"
						+ "\t\t\tValor por defecto: La generación de números aleatorios no se basa en ninguna semilla.\n"
						+ "\t-h|--help\tImprime por consola la lista de posibles argumentos con su explicación.\n");
				System.exit(0);
			}
	}

}
