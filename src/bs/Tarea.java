package bs;

public class Tarea {

	private static double tLastArrive = 0;

	private long id;
	private double size;
	private double tArrive;

	public Tarea(long id, double size, double tArrive) {
		this.id = id;
		this.size = size;
		this.tArrive = tArrive;
	}

	public long getId() {
		return id;
	}

	public double getSize() {
		return size;
	}

	public double gettArrive() {
		return tArrive;
	}

	public static double getDelay(double tArrive) {
		double a = tArrive - tLastArrive;
		tLastArrive = tArrive;
		return a;
	}
}
