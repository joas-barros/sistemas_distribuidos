package edu.unidade2.minidns.src.server;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImplMiniDNSServer {
    DatagramSocket serverSocket;
    private InetAddress host;
    private int port;

    public ImplMiniDNSServer(int port) {
        this.port = port;
        this.run(port);
    }

    private static final Map<String, String> dnsMap = new ConcurrentHashMap<>();

    private void run(int port) {
        try (DatagramSocket serverSocket = new DatagramSocket(port)) {
            host = InetAddress.getLocalHost();
            System.out.println("Servidor MiniDNS está rodando no endereço: " + host + ":" + port);
            initServer();

            while (true) {
                byte[] receiveBuffer = new byte[1024];

                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);

                String request = new String(receivePacket.getData(), 0, receivePacket.getLength());
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                System.out.println("REQUISIÇÂO RECEBIDA: [" + request + "]" );

                String response = "";

                if(request.startsWith("UPDATE:")){
                    String[] parts = request.split(":");

                    String serverName = parts[1];
                    String newIp = parts[2];

                    if(!dnsMap.containsKey(serverName)){
                        response = "Servidor não encontrado para atualização";
                    } else {
                        dnsMap.put(serverName, newIp);
                        response = "Endereço IP do " + serverName + " atualizado para " + newIp;
                    }

                } else if (request.startsWith("GET")) {
                    String serverName = request.split(":")[1];
                    response = dnsMap.getOrDefault(serverName, "Servidor não encontrado");
                }

                byte[] sendBuffer = response.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, clientAddress, clientPort);
                serverSocket.send(sendPacket);
                System.out.println("RESPOSTA ENVIADA: [" + response + "]" );
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        }
    }

    private void initServer() {
        dnsMap.put("servidor1", "192.168.0.10");
        dnsMap.put("servidor2", "192.168.0.20");
        dnsMap.put("servidor3", "192.168.0.30");
        dnsMap.put("servidor4", "192.168.0.40");
        dnsMap.put("servidor5", "192.168.0.50");
        dnsMap.put("servidor6", "192.168.0.60");
        dnsMap.put("servidor7", "192.168.0.70");
        dnsMap.put("servidor8", "192.168.0.80");
        dnsMap.put("servidor9", "192.168.0.90");
        dnsMap.put("servidor10", "192.168.0.100");
    }
}
