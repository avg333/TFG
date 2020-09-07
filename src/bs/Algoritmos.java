package bs;

import java.util.Map;

public class Algoritmos {

	public static final char NO_COALESCING = 'n';
	public static final char SIZE_BASED_COALESCING = 's';
	public static final char TIME_BASED_COALESCING = 't';
	public static final char FIXED_COALESCING = 'f';

	public static double algoritmoProcesado(BaseStation bs) {
		double tProcesado = -1;

		if (!bs.procesando && !bs.listaTareasPendientes.isEmpty() && bs.state == BaseStation.ON) {
			bs.tareaProcesandose = bs.listaTareasPendientes.pollFirstEntry().getValue();
			bs.procesando = true;
			tProcesado = bs.tareaProcesandose.getSize() / bs.c;

			if (!bs.listaTareasPendientes.isEmpty())
				bs.q -= bs.tareaProcesandose.getSize();
			else
				bs.q = 0.0;
		}

		return tProcesado;
	}

	public static double algoritmoSuspension(BaseStation bs) {
		double tNewState = 0;

		if (bs.state == BaseStation.ON && bs.listaTareasPendientes.isEmpty() && !bs.procesando) {
			if (bs.tToOff == 0 && bs.tHysterisis == 0) {
				bs.state = BaseStation.OFF;
				if (bs.algorithm == FIXED_COALESCING) {
					bs.nextState = BaseStation.OFF;
					tNewState = bs.algorithmParam;
				}
			} else if (bs.tHysterisis == 0) {
				bs.state = BaseStation.TO_OFF;
				bs.nextState = BaseStation.OFF;
				tNewState = bs.tToOff;
			} else if (bs.tToOff == 0) {
				bs.state = BaseStation.HISTERISIS;
				bs.nextState = BaseStation.OFF;
				tNewState = bs.tHysterisis;
			} else {
				bs.state = BaseStation.HISTERISIS;
				bs.nextState = BaseStation.TO_OFF;
				tNewState = bs.tHysterisis;
			}
		}

		return tNewState;
	}

	public static double algoritmoActivacion(BaseStation bs, boolean newState) {
		double tNewState = 0;

		switch (bs.algorithm) {
		case NO_COALESCING:
			return algoritmoActivacionNoCoalescing(bs);
		case SIZE_BASED_COALESCING:
			return algoritmoActivacionSizeBasedCoalescing(bs);
		case TIME_BASED_COALESCING:
			return algoritmoActivacionTimeBasedCoalescing(bs);
		case FIXED_COALESCING:
			return algoritmoActivacionFixedCoalescing(bs, newState);
		}

		return tNewState;
	}

	private static double algoritmoActivacionNoCoalescing(BaseStation bs) {
		double tNewState = 0;

		switch (bs.state) {
		case BaseStation.OFF:
			if (!bs.listaTareasPendientes.isEmpty()) {
				if (bs.tToOn == 0)
					bs.state = BaseStation.ON;
				else {
					bs.state = BaseStation.TO_ON;
					bs.nextState = BaseStation.ON;
					tNewState = bs.tToOn;
				}
			}
			break;
		case BaseStation.HISTERISIS:
			if (!bs.listaTareasPendientes.isEmpty())
				bs.state = BaseStation.ON;
			break;
		}

		return tNewState;
	}

	private static double algoritmoActivacionSizeBasedCoalescing(BaseStation bs) {
		double tNewState = 0;
		double q = 0;

		for (Map.Entry<Long, Tarea> entry : bs.listaTareasPendientes.entrySet())
			q += entry.getValue().getSize();

		switch (bs.state) {
		case BaseStation.OFF:
			if (q > bs.algorithmParam) {
				if (bs.tToOn == 0)
					bs.state = BaseStation.ON;
				else {
					bs.state = BaseStation.TO_ON;
					bs.nextState = BaseStation.ON;
					tNewState = bs.tToOn;
				}
			}
			break;
		case BaseStation.HISTERISIS:
			if (!bs.listaTareasPendientes.isEmpty())
				bs.state = BaseStation.ON;
			break;
		}

		return tNewState;
	}

	private static double algoritmoActivacionTimeBasedCoalescing(BaseStation bs) {
		double tNewState = 0;

		switch (bs.state) {
		case BaseStation.OFF:
			if (!bs.listaTareasPendientes.isEmpty()) {
				if (bs.tToOn == 0 && bs.algorithmParam == 0)
					bs.state = BaseStation.ON;
				else if (bs.tToOn != 0 && bs.algorithmParam == 0) {
					bs.state = BaseStation.TO_ON;
					bs.nextState = BaseStation.ON;
					tNewState = bs.tToOn;
				} else if (bs.tToOn == 0 && bs.algorithmParam != 0) {
					bs.state = BaseStation.WAITING_TO_ON;
					bs.nextState = BaseStation.ON;
					tNewState = bs.algorithmParam;
				} else {
					bs.state = BaseStation.WAITING_TO_ON;
					bs.nextState = BaseStation.TO_ON;
					tNewState = bs.algorithmParam;
				}
			}
			break;
		case BaseStation.HISTERISIS:
			if (!bs.listaTareasPendientes.isEmpty())
				bs.state = BaseStation.ON;
			break;
		}

		return tNewState;
	}

	private static double algoritmoActivacionFixedCoalescing(BaseStation bs, boolean newState) {
		double tNewState = 0;

		switch (bs.state) {
		case BaseStation.OFF:
			if (!bs.listaTareasPendientes.isEmpty() && newState) {
				if (bs.tToOn == 0)
					bs.state = BaseStation.ON;
				else {
					bs.state = BaseStation.TO_ON;
					bs.nextState = BaseStation.ON;
					tNewState = bs.tToOn;
				}
			} else if (newState) {
				bs.state = BaseStation.OFF;
				bs.nextState = BaseStation.OFF;
				tNewState = bs.algorithmParam;
			}
			break;
		case BaseStation.HISTERISIS:
			if (!bs.listaTareasPendientes.isEmpty())
				bs.state = BaseStation.ON;
			break;
		}

		return tNewState;
	}

}
