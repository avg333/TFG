package broker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

public class Logger {

	private static final int AVANCE = 1;
	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
	private static final Date date = new Date();
	private static final DecimalFormat DF_CSV = new DecimalFormat();
	private static final DecimalFormat DF_LOG = new DecimalFormat("#.###");
	private static final String SEPARADOR = ";";
	private static final String COLUMNAS = "T" + SEPARADOR + "ENTIDAD" + SEPARADOR + "ID" + SEPARADOR + "EVENTO"
			+ SEPARADOR + "TAREA" + SEPARADOR + "L" + SEPARADOR + "A" + SEPARADOR + "X" + SEPARADOR + "Y" + SEPARADOR
			+ "FROM-UE" + SEPARADOR + "TO-BS" + SEPARADOR + "Q" + SEPARADOR + "W" + SEPARADOR + "STATE";

	private static int aux = AVANCE;
	private static boolean verbosity;
	private static boolean eventos;
	private static ArrayList<String> listaEventos = new ArrayList<>();

	public static void setSettings(boolean verbosity, boolean eventos) {
		Logger.verbosity = verbosity;
		Logger.eventos = eventos;
	}

	public static void printProgress(double current, double total) {
		if (!(current / total * 100 > aux) || verbosity) {
			return;
		}

		aux += AVANCE;

		StringBuilder string = new StringBuilder(140);
		int percent = (int) (current * 100 / total);
		string.append('\r')
				.append(String.join("", Collections.nCopies(percent == 0 ? 2 : 2 - (int) (Math.log10(percent)), " ")))
				.append(String.format(" %d%% [", percent)).append(String.join("", Collections.nCopies(percent, "=")))
				.append('>').append(String.join("", Collections.nCopies(100 - percent, " "))).append(']')
				.append(String.join("",
						Collections.nCopies(current == 0 ? (int) (Math.log10(total))
								: (int) (Math.log10(total)) - (int) (Math.log10(current)), " ")))
				.append(String.format(" %d/%d", (int) current, (int) total));

		System.out.print(string);
	}

	public static void imprimirResultados(long elapsedTime) {
		System.out.println("\nFin de la simulación. Tiempo de ejecución: " + elapsedTime / 1000 + "s");
		imprimirResumen(elapsedTime);
		imprimirEventos();
	}

	public static void imprimirResumen(long elapsedTime) {
		double eQ = 0, eW = 0, eL = 0, eA = 0;

		for (Map.Entry<Integer, Bs> entry : Broker.listaBS.entrySet()) {
			Bs bsAux = entry.getValue();
			eQ += bsAux.getEq();
			eW += bsAux.getEw();
		}

		eQ = eQ / Broker.listaBS.size();
		eW = eW / Broker.listaBS.size();

		for (Map.Entry<Integer, Ue> entry : Broker.listaUE.entrySet()) {
			Ue ueAux = entry.getValue();
			eL += ueAux.geteL();
			eA += ueAux.geteA();
		}

		eL = eL / Broker.listaUE.size();
		eA = eA / Broker.listaUE.size();

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("resumen_" + formatter.format(date) + ".txt"));
			writer.write("Resumen simulación " + formatter.format(date) + ":\n");
			writer.write("Log: " + verbosity + ". Eventos: " + eventos + ". T final: " + DF_LOG.format(Broker.t)
					+ ". Tiempo de simulación: " + DF_LOG.format(elapsedTime / 1000) + "s.\n");
			writer.write("N: " + Broker.listaBS.size() + ". E[Q] Global: " + DF_LOG.format(eQ) + ". E[W] Global: "
					+ DF_LOG.format(eW) + ".\n");
			for (Map.Entry<Integer, Bs> entry : Broker.listaBS.entrySet())
				writer.write(
						"\tBS ID: " + entry.getValue().getId() + " E[Q]: " + DF_LOG.format(entry.getValue().getEq())
								+ " E[W]: " + DF_LOG.format(entry.getValue().getEw()) + ".\n");
			writer.write("m: " + Broker.listaUE.size() + ". E[L] Global: " + DF_LOG.format(eL) + ". E[A] Global: "
					+ DF_LOG.format(eA) + ".\n");
			for (Map.Entry<Integer, Ue> entry : Broker.listaUE.entrySet())
				writer.write(
						"\tUE ID: " + entry.getValue().getId() + " E[L]: " + DF_LOG.format(entry.getValue().geteL())
								+ " E[A]: " + DF_LOG.format(entry.getValue().geteA()) + ".\n");
			writer.close();
		} catch (IOException e1) {
			System.out.println("Error al imprimir el archivo resumen.txt.");
		}
	}

	public static void imprimirEventos() {
		if (listaEventos.isEmpty())
			return;

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("eventos_" + formatter.format(date) + ".csv"));
			writer.write(COLUMNAS + "\n");
			for (int i = 0; i < listaEventos.size(); i++)
				writer.write(listaEventos.get(i) + "\n");
			writer.close();
		} catch (IOException e1) {
			System.out.println("Error al imprimir el archivo eventos.csv.");
		}
	}

	public static void logTRAFFIC_INGRESS(double t, int idUe, double xUe, double yUe, long idTarea, double size,
			double delay) {
		if (verbosity)
			System.out.println(DF_LOG.format(t) + " UE " + idUe + " TRAFFIC_INGRESS id=" + idTarea + " size="
					+ DF_LOG.format(size) + " next=" + DF_LOG.format(delay) + " x=" + DF_LOG.format(xUe) + " y="
					+ DF_LOG.format(yUe));
		if (eventos)
			listaEventos.add(DF_CSV.format(t) + SEPARADOR + "UE" + SEPARADOR + idUe + SEPARADOR + "TRAFFIC_INGRESS"
					+ SEPARADOR + idTarea + SEPARADOR + DF_CSV.format(size) + SEPARADOR + DF_CSV.format(delay)
					+ SEPARADOR + DF_CSV.format(xUe) + SEPARADOR + DF_CSV.format(yUe));
	}

	public static void logTRAFFIC_ROUTE(double t, int idUe, int idBs, long idTarea, double size) {
		if (verbosity)
			System.out.println(DF_LOG.format(t) + " BK 0 TRAFFIC_ROUTE id=" + idTarea + " size=" + DF_LOG.format(size)
					+ " from-ue=" + idUe + " to-bs=" + idBs);
		if (eventos)
			listaEventos.add(DF_CSV.format(t) + SEPARADOR + "BK" + SEPARADOR + "0" + SEPARADOR + "TRAFFIC_ROUTE"
					+ SEPARADOR + idTarea + SEPARADOR + DF_CSV.format(size) + SEPARADOR + SEPARADOR + SEPARADOR
					+ SEPARADOR + idUe + SEPARADOR + idBs);
	}

	public static void logTRAFFIC_ARRIVAL(double t, int idBs, long idDemanda, double cantidad, double cola, double a) {
		if (verbosity)
			System.out.println(DF_LOG.format(t) + " BS " + idBs + " TRAFFIC_ARRIVAL id=" + idDemanda + " size="
					+ DF_LOG.format(cantidad) + " a=" + DF_LOG.format(a) + " q=" + DF_LOG.format(cola));
		if (eventos)
			listaEventos.add(DF_CSV.format(t) + SEPARADOR + "BS" + SEPARADOR + idBs + SEPARADOR + "TRAFFIC_ARRIVAL"
					+ SEPARADOR + idDemanda + SEPARADOR + DF_CSV.format(cantidad) + SEPARADOR + DF_CSV.format(a)
					+ SEPARADOR + SEPARADOR + SEPARADOR + SEPARADOR + SEPARADOR + DF_CSV.format(cola) + SEPARADOR
					+ SEPARADOR);
	}

	public static void logTRAFFIC_EGRESS(double t, int idBs, long idDemanda, double cantidad, double cola,
			double wait) {
		if (verbosity)
			System.out.println(DF_LOG.format(t) + " BS " + idBs + " TRAFFIC_EGRESS id=" + idDemanda + " size="
					+ DF_LOG.format(cantidad) + " q=" + DF_LOG.format(cola) + " wait=" + DF_LOG.format(wait));
		if (eventos)
			listaEventos.add(DF_CSV.format(t) + SEPARADOR + "BS" + SEPARADOR + idBs + SEPARADOR + "TRAFFIC_EGRESS"
					+ SEPARADOR + idDemanda + SEPARADOR + DF_CSV.format(cantidad) + SEPARADOR + SEPARADOR + SEPARADOR
					+ SEPARADOR + SEPARADOR + SEPARADOR + DF_CSV.format(cola) + SEPARADOR + DF_CSV.format(wait)
					+ SEPARADOR);
	}

	public static void logNEW_STATE(double t, int idBs, double q, int estadoBs) {
		String state = "";
		switch (estadoBs) {
		case Bs.ON:
			state = "on";
			break;
		case Bs.OFF:
			state = "off";
			break;
		case Bs.TO_ON:
			state = "to_on";
			break;
		case Bs.TO_OFF:
			state = "to_off";
			break;
		case Bs.HISTERISIS:
		case Bs.WAITING_TO_ON:
			return;
		}

		if (verbosity)
			System.out
					.println(DF_LOG.format(t) + " BS " + idBs + " NEW_STATE q=" + DF_LOG.format(q) + " state=" + state);
		if (eventos)
			listaEventos.add(DF_CSV.format(t) + SEPARADOR + "BS" + SEPARADOR + idBs + SEPARADOR + "NEW_STATE"
					+ SEPARADOR + SEPARADOR + SEPARADOR + SEPARADOR + SEPARADOR + SEPARADOR + SEPARADOR + SEPARADOR
					+ DF_CSV.format(q) + SEPARADOR + SEPARADOR + state);
	}

}
