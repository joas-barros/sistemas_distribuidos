package edu.unidade2.servicediscovery.src.server.proxy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadDirectory implements Runnable {

    private Socket clientSocket;
    private List<Integer> serverPorts;

    // Thread safe
    private AtomicInteger counter;
    private  AtomicInteger clientCounter;

    public ThreadDirectory(Socket clientSocket, List<Integer> serverPorts, AtomicInteger counter, AtomicInteger clientCounter) {
        this.clientSocket = clientSocket;
        this.serverPorts = serverPorts;
        this.counter = counter;
        this.clientCounter = clientCounter;
    }

    @Override
    public void run() {

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintStream out = new PrintStream(clientSocket.getOutputStream());

            String request = in.readLine();

            if (request == null){
                return; // Cliente desconectado
            }

            if (request.startsWith("REGISTER")) {
                String[] parts = request.split(" ");
                int port = Integer.parseInt(parts[1]);

                if (!serverPorts.contains(port)) {
                    serverPorts.add(port);
                    System.out.println("Servidor de cálculo registrado na porta: " + port);
                } else {
                    System.out.println("Servidor na porta " + port + " já estava registrado.");
                }

            } else if (request.equals("GET PORT")) {
                if (serverPorts.isEmpty()) {
                    out.println("NO SERVERS AVAILABLE");
                    System.out.println("Nenhum servidor disponível para fornecer a porta.");
                } else {

                    int clienteId = clientCounter.incrementAndGet();
                    String clienteNome = "Cliente " + clienteId;

                    int index = counter.getAndIncrement() % serverPorts.size();
                    int port = serverPorts.get(index);
                    out.println("PORT " + port + " ID " + clienteId);
                    System.out.println(clienteNome + " [ " + clientSocket.getInetAddress().getHostAddress() +
                            " ] foi direcionado para a porta: " + port);
                }
            } else {
                out.println("INVALID REQUEST");
                System.out.println("Requisição inválida recebida: " + request);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
