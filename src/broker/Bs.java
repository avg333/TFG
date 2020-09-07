package broker;

import java.net.DatagramSocket;
import java.net.InetAddress;

public class Bs extends Entidad {

	public static final int ON = 1;
	public static final int OFF = 2;
	public static final int TO_ON = 3;
	public static final int TO_OFF = 4;
	public static final int HISTERISIS = -1;
	public static final int WAITING_TO_ON = -2;

	private int estado = 0;
	private double q = 0.0;
	private double eW = 0.0;
	private double eQ = 0.0;

	private int nextStateBs = OFF;
	private long idEventoNextState;
	private long contadorW = 0;
	private double qAux = 0.0;
	private double tAux = 0.0;

	public Bs(int id, double x, double y, DatagramSocket sc, InetAddress ad, int puerto) {
		super(id, x, y, sc, ad, puerto);
	}

	public int getEstado() {
		return estado;
	}

	public double getQ() {
		return q;
	}

	public double getEw() {
		if (contadorW == 0)
			return 0;
		
		return eW / contadorW;
	}

	public double getEq() {
		if (tAux == 0)
			return 0;
		
		return eQ / tAux;
	}

	public int getNextStateBs() {
		return nextStateBs;
	}

	public long getIdEventoNextState() {
		return idEventoNextState;
	}

	public void setEstado(int estado) {
		this.estado = estado;
	}

	public void setNextState(int nextStateBs) {
		this.nextStateBs = nextStateBs;
	}

	public void setIdEventoNextState(long idEventoNextState) {
		this.idEventoNextState = idEventoNextState;
	}

	public void addW(double w) {
		this.eW += w;
		this.contadorW++;
	}

	public void addQ(double q, double t) {
		double intervalo = t - tAux;
		this.q = q;
		this.eQ += qAux * intervalo;
		this.qAux = q;
		this.tAux = t;
	}

}
