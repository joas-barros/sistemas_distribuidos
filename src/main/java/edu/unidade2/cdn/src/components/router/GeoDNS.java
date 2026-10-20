package edu.unidade2.cdn.src.components.router;

import edu.unidade2.cdn.src.config.Color;
import edu.unidade2.security.handler.HandshakeHandler;
import edu.unidade2.security.SecureMessageFacade;
import edu.unidade2.security.handler.UdpSecurityHandler;
import edu.unidade2.security.keys.KeyGeneratorUtil;
import edu.unidade2.security.model.SessionKeys;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class GeoDNS {
    private final int udpPort;
    private final int tcpPort;

    private final List<String> activeEdges = new CopyOnWriteArrayList<>();
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    private KeyPair rsaKeyPair;

    public GeoDNS(int udpPort, int tcpPort) {
        this.udpPort = udpPort;
        this.tcpPort = tcpPort;

        start();
    }

    private void start() {
        try {
            System.out.println(Color.infoMessage("[GeoDNS] Gerando chaves RSA..."));
            this.rsaKeyPair = KeyGeneratorUtil.gerarParRSA();
        } catch (Exception e) {
            System.out.println(Color.securityMessage("Erro fatal ao gerar chaves RSA: " + e.getMessage()));
            System.exit(1);
        }

        new Thread(this::listenForEdgesTCP).start();
        new Thread(this::listenForClientsUDP).start();
    }

    private void listenForClientsUDP() {
        try (DatagramSocket udpSocket = new DatagramSocket(udpPort)) {
            byte[] buffer = new byte[4096];

            System.out.println(Color.infoMessage("Escutando clientes UDP na porta " + udpPort + "..."));

            while (true) {
                try {
                    DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(requestPacket);

                    String request = new String(requestPacket.getData(), 0, requestPacket.getLength()).trim();

                    // O Cliente pede a chave pública (Texto puro)
                    if (request.equals("GET_PUB_KEY")) {
                        String pubKeyBase64 = Base64.getEncoder().encodeToString(rsaKeyPair.getPublic().getEncoded());
                        byte[] responseBytes = pubKeyBase64.getBytes();

                        DatagramPacket responsePacket = new DatagramPacket(
                                responseBytes, responseBytes.length,
                                requestPacket.getAddress(), requestPacket.getPort()
                        );
                        udpSocket.send(responsePacket);
                        System.out.println(Color.infoMessage("[GeoDNS] Chave pública RSA enviada ao cliente."));
                        continue; // Volta ao início do loop para aguardar a requisição cifrada
                    }

                    // O Cliente envia a requisição cifrada
                    UdpSecurityHandler.UdpRequest udpReq = UdpSecurityHandler.processarRequisicaoSegura(request, rsaKeyPair);

                    System.out.println(Color.successMessage("[GeoDNS] Recebido do cliente (Seguro): " + udpReq.mensagemPlana()));

                    String responseStr = getNextEdge();
                    if (responseStr == null) {
                        responseStr = "ERROR: Nenhum Edge Server disponivel";
                    }

                    // Empacota a resposta cifrando com AES e assinando com HMAC
                    String respostaCifrada = SecureMessageFacade.empacotarMensagem(
                            responseStr,
                            udpReq.keys().aes(),
                            udpReq.keys().hmac()
                    );

                    byte[] responseBytes = respostaCifrada.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(
                            responseBytes, responseBytes.length,
                            requestPacket.getAddress(), requestPacket.getPort()
                    );
                    udpSocket.send(responsePacket);

                    System.out.println(Color.successMessage("[GeoDNS] Respondido ao cliente (Seguro)."));

                } catch (IllegalArgumentException | SecurityException e) {
                    // Captura falhas de HMAC ou pacotes malformados sem derrubar o servidor
                    System.out.println(Color.securityMessage("[GeoDNS] Pacote UDP descartado (Violação de Segurança): " + e.getMessage()));
                } catch (Exception e) {
                    System.out.println(Color.errorMessage("[GeoDNS] Erro ao processar pacote do cliente: " + e.getMessage()));
                }
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
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {

            SessionKeys keys = HandshakeHandler.realizarComoServidor(in, out, rsaKeyPair);

            String pacoteRequisitado = in.readLine();
            if (pacoteRequisitado == null) return;
            String command = SecureMessageFacade.desempacotarMensagem(pacoteRequisitado, keys.aes(), keys.hmac());

            if (command.startsWith("REGISTER ")) {
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
        } catch (SecurityException e) {
            System.out.println(Color.securityMessage("[GeoDNS] Violação de segurança na conexão do Edge Server: " + e.getMessage()));
        }
        catch (Exception e) {
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
