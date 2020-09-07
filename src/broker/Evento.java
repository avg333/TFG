package broker;

public class Evento {
	
	public static final int TRAFFIC_INGRESS = 1;
	public static final int TRAFFIC_ROUTE = 2;
	public static final int TRAFFIC_ARRIVE = 3;
	public static final int TRAFFIC_EGRESS = 4;
	public static final int NEW_STATE = 5;
	public static final int CLOSE = -1;
	
	private static long contador = 0;

	private long id;
	private int tipo;
	private double t;
	private Entidad entidad;
	
	public Evento(int tipo, double t, Entidad entidad) {
		this.id = contador++;
		this.tipo = tipo;
		this.t = t;
		this.entidad = entidad;
	}
	
	public long getId() {
		return id;
	}
	
	public int getTipo() {
		return tipo;
	}

	public double getT() {
		return t;
	}
	
	public Entidad getEntidad() {
		return entidad;
	}

}
