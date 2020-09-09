package broker;

import java.net.DatagramSocket;
import java.net.InetAddress;

public class Ue extends Entidad {
	private double eL = 0.0;
	private double eA = 0.0;
	private long contadorTareas = 0;

	public Ue(int id, double x, double y, DatagramSocket sc, InetAddress ad, int puerto) {
		super(id, x, y, sc, ad, puerto);
	}

	public void addTarea(double x, double y, double l, double a) {
		setX(x);
		setY(y);
		eL += l;
		eA += a;
		contadorTareas++;
	}

	public double geteL() {
		if (contadorTareas == 0)
			return 0;

		return eL / contadorTareas;
	}

	public double geteA() {
		if (contadorTareas == 0)
			return 0;

		return eA / contadorTareas;
	}

}
