package edu.unidade2.minidns.src.server;

import edu.unidade2.cdn.src.config.Color;
import edu.unidade2.security.SecureMessageFacade;
import edu.unidade2.security.handler.UdpSecurityHandler;
import edu.unidade2.security.keys.KeyGeneratorUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.KeyPair;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MiniDNSServer {
    private DatagramSocket serverSocket;
    private InetAddress host;
    private int port;
    private KeyPair rsaKeyPair;

    private static final Map<String, String> dnsMap = new ConcurrentHashMap<>();

    public MiniDNSServer(int port) {
        this.port = port;
        try {
            System.out.println(Color.securityMessage("[MiniDNS] Gerando chaves RSA..."));
            this.rsaKeyPair = KeyGeneratorUtil.gerarParRSA();
        } catch (Exception e) {
            System.out.println(Color.errorMessage("Erro fatal ao gerar chaves RSA: " + e.getMessage()));
            System.exit(1);
        }
        this.run(port);
    }

    private void run(int port) {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            this.serverSocket = socket;
            host = InetAddress.getLocalHost();
            System.out.println(Color.infoMessage("Servidor MiniDNS (Seguro) está rodando em: " + host + ":" + port));
            initServer();

            while (true) {
                try {
                    byte[] receiveBuffer = new byte[4096]; // Aumentado para comportar o pacote RSA+AES+HMAC
                    DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                    serverSocket.receive(receivePacket);

                    String request = new String(receivePacket.getData(), 0, receivePacket.getLength()).trim();
                    InetAddress clientAddress = receivePacket.getAddress();
                    int clientPort = receivePacket.getPort();

                    // Fornecer chave pública se solicitado
                    if (request.equals("GET_PUB_KEY")) {
                        String pubKey = Base64.getEncoder().encodeToString(rsaKeyPair.getPublic().getEncoded());
                        byte[] sendBuffer = pubKey.getBytes();
                        serverSocket.send(new DatagramPacket(sendBuffer, sendBuffer.length, clientAddress, clientPort));
                        System.out.println(Color.infoMessage("[MiniDNS] Chave Pública RSA enviada para " + clientAddress));
                        continue;
                    }

                    // Desempacotar requisição segura e validar HMAC
                    UdpSecurityHandler.UdpRequest udpReq = UdpSecurityHandler.processarRequisicaoSegura(request, rsaKeyPair);
                    String plainRequest = udpReq.mensagemPlana();

                    System.out.println(Color.successMessage("[MiniDNS] REQUISIÇÃO RECEBIDA E HMAC VALIDADO: [" + plainRequest + "]"));

                    String response = processarComando(plainRequest);

                    // Empacotar e enviar resposta cifrada
                    String respostaCifrada = SecureMessageFacade.empacotarMensagem(response, udpReq.keys().aes(), udpReq.keys().hmac());
                    byte[] sendBuffer = respostaCifrada.getBytes();

                    serverSocket.send(new DatagramPacket(sendBuffer, sendBuffer.length, clientAddress, clientPort));
                    System.out.println(Color.successMessage("[MiniDNS] RESPOSTA SEGURA ENVIADA."));

                } catch (SecurityException se) {
                    System.out.println(Color.securityMessage("Violação de segurança detectada: " + se.getMessage()));
                } catch (Exception e) {
                    System.out.println(Color.errorMessage("Erro ao processar pacote UDP: " + e.getMessage()));
                }
            }
        } catch (Exception e) {
            System.err.println("Erro crítico no servidor MiniDNS: " + e.getMessage());
        }
    }

    private String processarComando(String request) {
        if(request.startsWith("UPDATE:")){
            String[] parts = request.split(":");
            String serverName = parts[1];
            String newIp = parts[2];

            if(!dnsMap.containsKey(serverName)){
                return "Servidor não encontrado para atualização";
            } else {
                dnsMap.put(serverName, newIp);
                return "Endereço IP do " + serverName + " atualizado para " + newIp;
            }
        } else if (request.startsWith("GET:")) {
            String serverName = request.split(":")[1];
            return dnsMap.getOrDefault(serverName, "Servidor não encontrado");
        }
        return "Requisição inválida";
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
