package broker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;

public class Depuracion {

	private static final DecimalFormat DF_CSV = new DecimalFormat();
	private static final String SEPARADOR = ";";

	private static final double AVANCE = 0.1;
	private static double aux = AVANCE;

	public static ArrayList<MyVector> lista = new ArrayList<>();

	public static ArrayList<String> progresUE = new ArrayList<>();
	public static ArrayList<String> progresBS = new ArrayList<>();

	public static void imprimirLista() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("log_TIME" + ".csv"));
			writer.write("X" + SEPARADOR + "Y\n");
			for (int i = 0; i < lista.size(); i++)
				writer.write(lista.get(i).getT() + SEPARADOR + lista.get(i).getTime() + "\n");
			writer.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public static void addVector(long t, long time) {
		if (t / 1000 >= aux) {
			MyVector vector = new MyVector(t, time);
			lista.add(vector);
			aux += AVANCE;
		}
	}

	static class MyVector {
		long t;
		long time;

		public MyVector(long t, long time) {
			this.t = t;
			this.time = time;
		}

		public long getT() {
			return t;
		}

		public long getTime() {
			return time;
		}
	}

	public static void addProgressUE(long t, double total, Ue ue) {
		if (!(t / total * 100 > aux)) {
			return;
		}
		aux += AVANCE;
		String entrada = DF_CSV.format(t) + SEPARADOR + DF_CSV.format(ue.geteL()) + SEPARADOR
				+ DF_CSV.format(ue.geteA()) + ";1;1,25" + SEPARADOR
				+ DF_CSV.format(Math.abs((ue.geteL() - 1) / 1.0 * 100)) + SEPARADOR
				+ DF_CSV.format(Math.abs((ue.geteA() - 1.25) / 1.25 * 100));
		progresUE.add(entrada);
	}

	public static void addProgressBS(long t, double total, Bs bs) {
		if (!(t / total * 100 > aux)) {
			return;
		}
		aux += AVANCE;
		String entrada = DF_CSV.format(t) + SEPARADOR + DF_CSV.format(bs.getEq()) + SEPARADOR
				+ DF_CSV.format(bs.getEw()) + ";3,2;4" + SEPARADOR
				+ DF_CSV.format(Math.abs((bs.getEq() - 3.2) / 3.2 * 100)) + SEPARADOR
				+ DF_CSV.format(Math.abs((bs.getEw() - 4.0) / 4.0 * 100));
		progresBS.add(entrada);
	}

	public static void imprimirProgresionUE() {
		String columnas = "T;E[L];E[A];E[L] Teórico;E[A] Teórico;Error E[L];Error E[A]";
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter("PROGRESSUE.csv"));
			writer.write(columnas + "\n");
			for (int i = 0; i < progresUE.size(); i++)
				writer.write(progresUE.get(i) + "\n");
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void imprimirProgresionBS() {
		String columnas = "T;E[Q];E[W];E[Q] Teórico;E[W] Teórico;Error E[Q];Error E[W]";
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter("PROGRESSBS.csv"));
			writer.write(columnas + "\n");
			for (int i = 0; i < progresBS.size(); i++)
				writer.write(progresBS.get(i) + "\n");
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void printGrid(Ue ue, Bs bs, int filas, int columnas) {

		char[][] mapa = new char[filas][columnas * 2];

		for (int i = 0; i < mapa.length; i++) {
			for (int j = 0; j < mapa[i].length; j++) {
				if (j % 2 == 0)
					mapa[i][j] = ' ';
				else
					mapa[i][j] = SEPARADOR.charAt(0);
			}
		}

		for (Map.Entry<Integer, Bs> entry : Broker.listaBS.entrySet()) {
			int x = (int) entry.getValue().getX();
			int y = (int) entry.getValue().getY();
			mapa[x][y * 2] = 'B';
		}

		for (Map.Entry<Integer, Ue> entry : Broker.listaUE.entrySet()) {
			int x = (int) entry.getValue().getX();
			int y = (int) entry.getValue().getY();
			mapa[x][y * 2] = 'U';
		}

		mapa[(int) ue.getX()][(int) ue.getY() * 2] = Character.forDigit(ue.getId(), 10);
		mapa[(int) bs.getX()][(int) bs.getY() * 2] = Character.forDigit(bs.getId(), 10);

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("ROUTE_GRID.csv"));
			for (int i = 0; i < mapa.length; i++) {
				writer.write(new String(mapa[i]) + "\n");
			}
			writer.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public static void printBSrouteList(Ue ue, Bs bs, long idTarea) {

		ArrayList<String> lista = new ArrayList<>();
		double distancia;
		boolean escogida = false;

		lista.add("idTarea" + SEPARADOR + "UE" + SEPARADOR + "UE_X" + SEPARADOR + "UE_Y" + SEPARADOR + "BS" + SEPARADOR
				+ "BS_X" + SEPARADOR + "BS_Y" + SEPARADOR + "Distancia" + SEPARADOR + "Escogida");

		for (Map.Entry<Integer, Bs> entry : Broker.listaBS.entrySet()) {
			Bs bsAux = entry.getValue();
			distancia = Broker.vectorDeDistancias(ue.getX(), ue.getY(), bsAux.getX(), bsAux.getY());
			if (bsAux.getId() == bs.getId())
				escogida = true;
			lista.add(idTarea + SEPARADOR + ue.getId() + SEPARADOR + DF_CSV.format(ue.getX()) + SEPARADOR
					+ DF_CSV.format(ue.getY()) + SEPARADOR + bsAux.getId() + SEPARADOR + DF_CSV.format(bsAux.getX())
					+ SEPARADOR + DF_CSV.format(bsAux.getY()) + SEPARADOR + DF_CSV.format(distancia) + SEPARADOR
					+ escogida);
			escogida = false;
		}

		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("TRAFFIC_ROUTE_" + idTarea + ".csv"));
			for (int i = 0; i < lista.size(); i++)
				writer.write(lista.get(i) + "\n");
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
