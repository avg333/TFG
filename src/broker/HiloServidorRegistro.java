package broker;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

public class HiloServidorRegistro extends Thread {

	public static final int CERRAR = 0;
	public static final int UE = 1;
	public static final int BS = 2;

	private static int puerto;
	private static DatagramSocket sc;

	public HiloServidorRegistro(int puerto) {
		HiloServidorRegistro.puerto = puerto;
	}

	@Override
	public void run() {
		System.out.print("Registradas las entidades:");
		try {
			sc = new DatagramSocket(puerto);
			while (true) {
				byte[] data = new byte[50];
				DatagramPacket dp = new DatagramPacket(data, data.length);
				sc.receive(dp);
				MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(dp.getData());
				int tipoEntidad = unpacker.unpackInt();

				if (tipoEntidad == CERRAR) {
					unpacker.close();
					return;
				}

				int id = unpacker.unpackInt();
				double x = unpacker.unpackDouble();
				double y = unpacker.unpackDouble();
				unpacker.close();

				if (tipoEntidad == UE) {
					Ue ue = new Ue(id, x, y, sc, dp.getAddress(), dp.getPort());
					Broker.listaUE.put(ue.getId(), ue);
					Evento trafficIngress = new Evento(Evento.TRAFFIC_INGRESS, Broker.t, ue);
					Broker.listaEventos.put(trafficIngress.getId(), trafficIngress);
					System.out.print(" UE_" + id);
				} else if (tipoEntidad == BS) {
					Bs bs = new Bs(id, x, y, sc, dp.getAddress(), dp.getPort());
					Broker.listaBS.put(bs.getId(), bs);
					Evento newState = new Evento(Evento.NEW_STATE, Broker.t, bs);
					Broker.listaEventos.put(newState.getId(), newState);
					System.out.print(" BS_" + id);
				}

			}
		} catch (IOException ex) {
			System.out.println("Error en el servidor de registro. Ejecución finalizada");
			System.exit(-1);
		}
	}

	public static void cerrarServidorRegistro() {
		MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
		try {
			DatagramSocket scAux = new DatagramSocket();
			InetAddress adR = InetAddress.getByName("localhost");
			packer.packInt(CERRAR).close();
			byte[] dataRequest = packer.toByteArray();
			scAux.send(new DatagramPacket(dataRequest, dataRequest.length, adR, puerto));
			scAux.close();
		} catch (IOException e) {
			System.out.println("Error al cerrar el servidor de registro. Ejecución finalizada");
			System.exit(-1);
		}
	}

	public static void cerrarSockets() {
		try {
			for (Map.Entry<Integer, Bs> entry : Broker.listaBS.entrySet())
				entry.getValue().cerrarSocket();

			for (Map.Entry<Integer, Ue> entry : Broker.listaUE.entrySet())
				entry.getValue().cerrarSocket();

			sc.close();
		} catch (Exception e) {
			System.out.println("Error al cerrar los sockets. Ejecución finalizada");
			System.exit(-1);
		}
	}

}
