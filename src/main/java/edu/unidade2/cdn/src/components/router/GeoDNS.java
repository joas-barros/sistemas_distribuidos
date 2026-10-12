package edu.unidade2.cdn.src.components.router;

import edu.unidade2.cdn.src.config.Color;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class GeoDNS {
    private final int udpPort;
    private final int tcpPort;

    private final List<String> activeEdges = new CopyOnWriteArrayList<>();
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    public GeoDNS(int udpPort, int tcpPort) {
        this.udpPort = udpPort;
        this.tcpPort = tcpPort;

        new Thread(this::listenForClientsUDP).start();
        new Thread(this::listenForEdgesTCP).start();
    }

    private void listenForClientsUDP() {
        try (DatagramSocket udpSocket = new DatagramSocket(udpPort)) {
            byte[] buffer = new byte[1024];

            System.out.println(Color.infoMessage("Escutando clientes UDP na porta " + udpPort + "..."));

            while (true) {
                DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(requestPacket);

                String request = new String(requestPacket.getData(), 0, requestPacket.getLength()).trim();
                System.out.println(Color.successMessage("[GeoDNS] Recebido do cliente: " + request));

                String responseStr = getNextEdge();
                if (responseStr == null) {
                    responseStr = "ERROR: Nenhum Edge Server disponivel";
                }

                byte[] responseBytes = responseStr.getBytes();

                DatagramPacket responsePacket = new DatagramPacket(
                        responseBytes, responseBytes.length,
                        requestPacket.getAddress(), requestPacket.getPort()
                );
                udpSocket.send(responsePacket);

                System.out.println(Color.successMessage("[GeoDNS] Respondido ao cliente: " + responseStr));
            }
        } catch (Exception e) {
            System.out.println(Color.errorMessage("[GeoDNS] Erro ao escutar clientes UDP: " + e.getMessage()));
        }
    }

    private void listenForEdgesTCP() {
        try (ServerSocket tcpSocket = new ServerSocket(tcpPort)) {
            System.out.println(Color.infoMessage("Escutando Edge Servers TCP na porta " + tcpPort + "..."));

            while (true) {
                var edgeSocket = tcpSocket.accept();
                new Thread(() -> handleEdgeConnection(edgeSocket)).start();
            }
        } catch (Exception e) {
            System.out.println(Color.errorMessage("[GeoDNS] Erro ao escutar Edge Servers TCP: " + e.getMessage()));
        }
    }

    private void handleEdgeConnection(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String command = in.readLine();
            if (command != null) {
                if (command.startsWith("REGISTER ")){
                    String edgeIp = command.substring(9).trim();
                    if (!activeEdges.contains(edgeIp)) {
                        activeEdges.add(edgeIp);
                        System.out.println(Color.successMessage("[GeoDNS] Edge Server registrado: " + edgeIp));
                    }
                } else if (command.startsWith("UNREGISTER ")) {
                    String edgeIp = command.substring(11).trim();
                    activeEdges.remove(edgeIp);
                    System.out.println(Color.warningMessage("[GeoDNS] Edge Server removido: " + edgeIp));
                } else {
                    System.out.println(Color.errorMessage("[GeoDNS] Comando desconhecido do Edge Server: " + command));
                }
            }
        } catch (Exception e) {
            System.out.println(Color.errorMessage("[GeoDNS] Erro ao lidar com conexão do Edge Server: " + e.getMessage()));
        }
    }

    private String getNextEdge() {
        if (activeEdges.isEmpty()) {
            return null;
        }

        int index = Math.abs(roundRobinIndex.getAndIncrement()) % activeEdges.size();
        return activeEdges.get(index);
    }
}
