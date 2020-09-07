package broker;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

public class Broker extends Thread {

	public static final char VECTOR_DE_DISTANCIAS = 'v';

	private static double T_FINAL;
	private static char algoritmo = VECTOR_DE_DISTANCIAS;

	private static long contadorTareas = 0;
	protected static double t = 0;
	protected static Map<Integer, Bs> listaBS = new TreeMap<>();
	protected static Map<Integer, Ue> listaUE = new TreeMap<>();
	protected static Map<Long, Evento> listaEventos = new TreeMap<>();

	public Broker(String args[]) {
		imprimirComandos(args);
		int puerto = 3000;
		boolean verbosity = false;
		boolean eventos = false;
		T_FINAL = Double.parseDouble(args[0]);

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-p") || args[i].equals("--port"))
				puerto = Integer.parseInt(args[++i]);
			else if (args[i].equals("-a") || args[i].equals("--algorithm"))
				algoritmo = args[++i].charAt(0);
			else if (args[i].equals("-v") || args[i].equals("--verbosity"))
				verbosity = true;
			else if (args[i].equals("-c") || args[i].equals("--csv"))
				eventos = true;
		}

		System.out.println("Broker iniciado con los parámetros:\n\tport=" + puerto + " algorithm=" + algoritmo
				+ " verbosity=" + verbosity + " csv=" + eventos);
		System.out.print("\nPulsa enter para iniciar la simulación. ");

		new HiloServidorRegistro(puerto).start();
		Logger.setSettings(verbosity, eventos);
	}

	public static void main(String[] args) {
		new Broker(args).run();
	}

	@Override
	public void run() {
		Scanner in = new Scanner(System.in);
		in.nextLine();
		in.close();
		HiloServidorRegistro.cerrarServidorRegistro();

		long start = System.currentTimeMillis();
		while (t <= T_FINAL) {
			Evento evento = obtenerProximoEvento();
			procesarEvento(evento);
			Logger.printProgress(t, T_FINAL);
			//Depuracion.addProgressUE(t, T_FINAL, listaUE.get(0));
			Depuracion.addProgressBS(t, T_FINAL, listaBS.get(0));
		}
		long finish = System.currentTimeMillis();

		HiloServidorRegistro.cerrarSockets();
		//Depuracion.imprimirProgresionUE();
		Depuracion.imprimirProgresionBS();
		Logger.imprimirResultados(finish - start);
	}

	private static Evento obtenerProximoEvento() {
		Evento evento = null;

		double t = -1;
		for (Map.Entry<Long, Evento> entry : listaEventos.entrySet()) {
			Evento eventoAux = entry.getValue();
			double tAux = eventoAux.getT();
			if (tAux < t || evento == null) {
				t = tAux;
				evento = eventoAux;
			}
		}

		return evento;
	}

	private static void procesarEvento(Evento evento) {
		if (evento == null)
			return;

		listaEventos.remove(evento.getId());
		t = evento.getT();

		try {
			switch (evento.getTipo()) {
			case Evento.TRAFFIC_INGRESS:
				procesarTRAFFIC_INGRESS(evento);
				break;
			case Evento.TRAFFIC_EGRESS:
				procesarTRAFFIC_EGRESS(evento);
				break;
			case Evento.NEW_STATE:
				procesarNEW_STATE(evento);
				break;
			}
		} catch (Exception e) {
			System.out.println("Error al intentar empaquetar/desempaquetar un mensaje. Ejecución finalizada");
			System.exit(-1);
		}

	}

	private static void procesarTRAFFIC_INGRESS(Evento evento) throws IOException {
		Ue ue = (Ue) evento.getEntidad();
		MessageBufferPacker requestTI = MessagePack.newDefaultBufferPacker();
		requestTI.packInt(Evento.TRAFFIC_INGRESS).close();
		MessageUnpacker responseTI = ue.comunicar(requestTI);

		long idTarea = contadorTareas++;
		double xUe = responseTI.unpackDouble();
		double yUe = responseTI.unpackDouble();
		double size = responseTI.unpackDouble();
		double delay = responseTI.unpackDouble();
		responseTI.close();

		Evento trafficIngress = new Evento(Evento.TRAFFIC_INGRESS, t + delay, ue);
		listaEventos.put(trafficIngress.getId(), trafficIngress);

		if (size == -1)
			return;

		ue.addTarea(xUe, yUe, size, delay);
		Logger.logTRAFFIC_INGRESS(t, ue.getId(), xUe, yUe, idTarea, size, delay);

		Bs bs = obtenerBS(xUe, yUe);
		Logger.logTRAFFIC_ROUTE(t, ue.getId(), bs.getId(), idTarea, size);

		/*
		 * -----------------------------------------------------------------------------
		 */
		MessageBufferPacker requestTA = MessagePack.newDefaultBufferPacker();
		requestTA.packInt(Evento.TRAFFIC_ARRIVE).packDouble(t).packLong(idTarea).packDouble(size);
		requestTA.close();
		MessageUnpacker responseTA = bs.comunicar(requestTA);
		
		double q = responseTA.unpackDouble();
		int state = responseTA.unpackInt();
		double tTrafficEgress = responseTA.unpackDouble();
		double tNewState = responseTA.unpackDouble();
		int nextState = responseTA.unpackInt();
		double a = responseTA.unpackDouble();
		responseTA.close();

		Logger.logTRAFFIC_ARRIVAL(t, bs.getId(), idTarea, size, q, a);

		if (bs.getEstado() == Bs.HISTERISIS) {
			Logger.logNEW_STATE(t, bs.getId(), q, state);
			listaEventos.remove(bs.getIdEventoNextState());
		} else if (state != bs.getEstado())
			Logger.logNEW_STATE(t, bs.getId(), q, state);

		crearEventos(bs, tNewState, tTrafficEgress, nextState);
		
		bs.addQ(q, t);
		bs.setEstado(state);
	}

	private static void procesarTRAFFIC_EGRESS(Evento evento) throws IOException {
		Bs bs = (Bs) evento.getEntidad();

		MessageBufferPacker requestTE = MessagePack.newDefaultBufferPacker();
		requestTE.packInt(Evento.TRAFFIC_EGRESS).packDouble(t).close();
		MessageUnpacker responseTE = bs.comunicar(requestTE);

		double q = responseTE.unpackDouble();
		int state = responseTE.unpackInt();
		double tTrafficEgress = responseTE.unpackDouble();
		double tNewState = responseTE.unpackDouble();
		int nextState = responseTE.unpackInt();
		double w = responseTE.unpackDouble();
		long id = responseTE.unpackLong();
		double size = responseTE.unpackDouble();
		responseTE.close();

		Logger.logTRAFFIC_EGRESS(t, bs.getId(), id, size, q, w);

		if (state != bs.getEstado())
			Logger.logNEW_STATE(t, bs.getId(), q, state);

		crearEventos(bs, tNewState, tTrafficEgress, nextState);

		bs.addQ(q, t);
		bs.addW(w);
		bs.setEstado(state);
	}

	private static void procesarNEW_STATE(Evento evento) throws IOException {
		Bs bs = (Bs) evento.getEntidad();
		int nextState = bs.getNextStateBs();

		MessageBufferPacker requestNS = MessagePack.newDefaultBufferPacker();
		requestNS.packInt(Evento.NEW_STATE).packInt(nextState).close();
		MessageUnpacker responseNS = bs.comunicar(requestNS);

		double q = responseNS.unpackDouble();
		int state = responseNS.unpackInt();
		double tTrafficEgress = responseNS.unpackDouble();
		double tNewState = responseNS.unpackDouble();
		nextState = responseNS.unpackInt();
		responseNS.close();

		//if (state != bs.getEstado())
			Logger.logNEW_STATE(t, bs.getId(), q, state);

		crearEventos(bs, tNewState, tTrafficEgress, nextState);
		
		bs.setEstado(state);
	}
	
	private static void crearEventos(Bs bs, double tNewState, double tTrafficEgress, int nextState) {
		if (tNewState > 0) {
			Evento newState = new Evento(Evento.NEW_STATE, t + tNewState, bs);
			listaEventos.put(newState.getId(), newState);
			bs.setNextState(nextState);
			bs.setIdEventoNextState(newState.getId());
		}

		if (tTrafficEgress > -1) {
			Evento trafficEgress = new Evento(Evento.TRAFFIC_EGRESS, t + tTrafficEgress, bs);
			listaEventos.put(trafficEgress.getId(), trafficEgress);
		}
	}

	private static Bs obtenerBS(double xUe, double yUe) {

		if (listaBS.isEmpty()) {
			System.out.println("Error: La lista de BS está vacía. Ejecución finalizada");
			System.exit(-1);
		}

		Bs bs = null;
		double distanciaMin = -1, distancia = 0;

		for (Map.Entry<Integer, Bs> entry : listaBS.entrySet()) {
			Bs bsAux = entry.getValue();
			switch (algoritmo) {
			case VECTOR_DE_DISTANCIAS:
				distancia = vectorDeDistancias(xUe, yUe, bsAux.getX(), bsAux.getY());
				break;
			}
			if (distancia < distanciaMin || bs == null) {
				distanciaMin = distancia;
				bs = bsAux;
			}
		}

		return bs;
	}

	public static double vectorDeDistancias(double xUe, double yUe, double xBs, double yBs) {
		double cateto1 = xUe - xBs;
		double cateto2 = yUe - yBs;
		double hipotenusa = Math.sqrt(cateto1 * cateto1 + cateto2 * cateto2);
		return hipotenusa;
	}

	
	private static void imprimirComandos(String args[]) {

		for (int i = 0; i < args.length; i++)
			if (args[i].equals("-h") || args[i].equals("--help")) {
				System.out.println("Argumentos obligatorios:\n"
						+ "\t<T_FINAL>\n"
						+ "Argumentos opcionales:\n"
						+ "\t[-p|--port <PUERTO>] [-a|--algorithm <ALGORITMO>] [-v|--verbosity] [-c|--csv] [-h|--help]\n"
						+ "Información sobre los argumentos:\n"
						+ "\t<T_FINAL>\tValor máximo de tiempo que puede alcanzar la simulación.\n"
						+ "\t-p|--port <PUERTO>\n"
						+ "\t\t\tCambia el puerto en el que se esperan comunicaciones de las entidades.\n"
						+ "\t\t\tValor por defecto: 3000.\n"
						+ "\t-a|--algorithm <ALGORITMO>\n"
						+ "\t\t\tDefine el algoritmo a usar para el encaminamiento de tareas. Valores permitidos:\n"
						+ "\t\t\t\tv[ector de distancias]\n"
						+ "\t\t\tValor por defecto: v.\n"
						+ "\t-c|--csv\tGenera un archivo csv al final de la simulación con todos los eventos y su información.\n"
						+ "\t-v|--verbosity\tImprime por consola los eventos con su información a medida que suceden en la simulación.\n"
						+ "\t-h|--help\tImprime por consola la lista de posibles argumentos con su explicación.\n");
				System.exit(0);
			}
	}

}
