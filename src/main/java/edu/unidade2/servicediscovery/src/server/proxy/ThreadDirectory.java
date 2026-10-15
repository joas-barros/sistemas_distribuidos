package edu.unidade2.servicediscovery.src.server.proxy;

import edu.unidade2.cdn.src.config.Color;
import edu.unidade2.security.SecureMessageFacade;
import edu.unidade2.security.handler.HandshakeHandler;
import edu.unidade2.security.model.SessionKeys;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadDirectory implements Runnable {

    private Socket clientSocket;
    private List<Integer> serverPorts;

    // Thread safe
    private AtomicInteger counter;
    private  AtomicInteger clientCounter;

    private KeyPair rsaKeyPair;

    public ThreadDirectory(Socket clientSocket, List<Integer> serverPorts, AtomicInteger counter, AtomicInteger clientCounter, KeyPair rsaKeyPair) {
        this.clientSocket = clientSocket;
        this.serverPorts = serverPorts;
        this.counter = counter;
        this.clientCounter = clientCounter;
        this.rsaKeyPair = rsaKeyPair;
    }

    @Override
    public void run() {

        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
                ){

            // Handshake seguro atuando como servidor
            SessionKeys keys = HandshakeHandler.realizarComoServidor(in, out, rsaKeyPair);

            // Recebe a requisição cifrada
            String encryptedRequest = in.readLine();
            if (encryptedRequest == null) {
                return; // Cliente desconectado
            }

            System.out.println(Color.securityMessage("[Server Discovery] Requisição recebida (cifrada): " + encryptedRequest));

            // Desempacota a requisição usando as chaves de sessão
            String request = SecureMessageFacade.desempacotarMensagem(encryptedRequest, keys.aes(), keys.hmac());

            System.out.println(Color.securityMessage("[Server Discovery] Requisição recebida (decifrada): " + request));

            // Processa a requisição do cliente
            if (request.startsWith("REGISTER")) {
                String[] parts = request.split(" ");
                int port = Integer.parseInt(parts[1]);

                // Adiciona a porta do servidor à lista se ainda não estiver presente
                if (!serverPorts.contains(port)) {
                    serverPorts.add(port);
                    System.out.println("Servidor de cálculo registrado na porta: " + port);
                } else {
                    System.out.println("Servidor na porta " + port + " já estava registrado.");
                }

                out.println(SecureMessageFacade.empacotarMensagem("REGISTER_OK", keys.aes(), keys.hmac()));

            } else if (request.equals("GET PORT")) { // Lida com uma requisição de porta

                // Verifica se há servidores disponíveis
                if (serverPorts.isEmpty()) {
                    out.println(SecureMessageFacade.empacotarMensagem("NO SERVERS AVAILABLE", keys.aes(), keys.hmac()));
                    System.out.println("Nenhum servidor disponível para fornecer a porta.");
                } else {

                    // Caso haja servidores disponíveis, atribui uma porta ao cliente e um id
                    int clienteId = clientCounter.incrementAndGet();
                    String clienteNome = "Cliente " + clienteId;

                    // Round Robin
                    int index = counter.getAndIncrement() % serverPorts.size();
                    int port = serverPorts.get(index);

                    String response = "PORT " + port + " ID " + clienteId;
                    out.println(SecureMessageFacade.empacotarMensagem(response, keys.aes(), keys.hmac()));

                    System.out.println(clienteNome + " [ " + clientSocket.getInetAddress().getHostAddress() +
                            " ] foi direcionado para a porta: " + port);
                }
            } else {
                out.println(SecureMessageFacade.empacotarMensagem("INVALID REQUEST", keys.aes(), keys.hmac()));
                System.out.println("Requisição inválida recebida: " + request);
            }
        } catch (SecurityException se) {
            System.out.println(Color.securityMessage(" Requisição rejeitada no Diretório: " + se.getMessage()));
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
