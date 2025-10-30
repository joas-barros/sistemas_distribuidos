package edu.unidade2.servicediscovery.src.server.proxy;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class DirectoryServer {
    private static List<Integer> serverPorts = new CopyOnWriteArrayList<>();

    private static AtomicInteger counter = new AtomicInteger(0);

    private AtomicInteger clientCounter = new AtomicInteger(0);

    public static final int BASE_PORT = 5000;



    public DirectoryServer() {
        this.run();
    }

    private void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(BASE_PORT);

            System.out.println("Servidor de diretório rodando na porta: " + BASE_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nova conexão recebida de: " + clientSocket.getInetAddress().getHostAddress());

                ThreadDirectory handler = new ThreadDirectory(clientSocket, serverPorts, counter, clientCounter);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
