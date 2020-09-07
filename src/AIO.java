import java.util.Random;

import broker.Broker;
import bs.BaseStation;
import ue.UserEquipment;

public class AIO {

	// PARAMETROS UE
	private static final int N = 1;
	private static final char SIZE_DISTR = 'e';
	private static final char DELAY_DISTR = 'e';
	private static final char MOBILITY_DISTR = 'd';
	private static final double SIZE_PARAM_1 = 1.0;
	private static final double SIZE_PARAM_2 = 0.0;
	private static final double DELAY_PARAM_1 = 0.8;
	private static final double DELAY_PARAM_2 = 0.0;
	private static final double MOBILITY_PARAM_1 = 0.0;
	private static final double MOBILITY_PARAM_2 = 0.0;
	private static final long SEMILLA = 3003;

	// PARAMETROS BS
	private static final int M = 1;
	private static final char ALGORITMO_BS = 'n';
	private static final double ALGORITMO_PARAM = 1.0;
	private static final double C = 1.0;
	private static final double T_TO_OFF = 0.0;
	private static final double T_TO_ON = 0.0;
	private static final double T_HISTERISIS = 0.0;

	// PARAMETROS BROKER
	private static final double T_FINAL = 5000.0 * 1000.0; // 100k -> 2min20s
	private static final int PUERTO = 3000;
	private static final char ALGORITMO_BROKER = 'v';
	private static final String LOG = "-+verbosity";
	private static final String LOG_CSV = "-+csv";

	// PARAMETROS GLOBALES
	private static final String IP_BROKER = "localhost";
	private static final int PUERTO_BROKER = PUERTO;
	private static final double POS_MAXIMO = 25.0;
	private static final double POS_MINIMO = -25.0;

	public static void main(String[] args) {
		String parametrosBROKER = T_FINAL + " -p " + PUERTO + " -a " + ALGORITMO_BROKER + " " + LOG + " " + LOG_CSV;
		new Broker(parametrosBROKER.split(" ")).start();

		for (int i = 0; i < M; i++) {
			String parametrosBS = i + " " + obtenerPosicionAleatoria() + " " + obtenerPosicionAleatoria() + " -h "
					+ IP_BROKER + " " + PUERTO_BROKER + " -c " + C + " -a " + ALGORITMO_BS + " " + ALGORITMO_PARAM
					+ " --times " + T_TO_OFF + " " + T_TO_ON + " " + T_HISTERISIS;
			new BaseStation(parametrosBS.split(" ")).start();
		}

		for (int i = 0; i < N; i++) {
			String parametrosUE = i + " " + obtenerPosicionAleatoria() + " " + obtenerPosicionAleatoria() + " -h "
					+ IP_BROKER + " " + PUERTO_BROKER + " -s " + SIZE_DISTR + " " + SIZE_PARAM_1 + " " + SIZE_PARAM_2
					+ " -d " + DELAY_DISTR + " " + DELAY_PARAM_1 + " " + DELAY_PARAM_2 + " -m " + MOBILITY_DISTR + " "
					+ MOBILITY_PARAM_1 + " " + MOBILITY_PARAM_2 + " --seed " + SEMILLA;
			new UserEquipment(parametrosUE.split(" ")).start();
		}

	}

	private static double obtenerPosicionAleatoria() {
		Random r = new Random();
		double x = (int) (r.nextDouble() * (POS_MAXIMO - POS_MINIMO) + POS_MINIMO);

		return x;
	}

}
