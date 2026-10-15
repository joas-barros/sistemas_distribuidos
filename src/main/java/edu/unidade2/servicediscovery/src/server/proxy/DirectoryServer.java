package edu.unidade2.servicediscovery.src.server.proxy;

import edu.unidade2.cdn.src.config.Color;
import edu.unidade2.security.keys.KeyGeneratorUtil;

import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class DirectoryServer {
    private static List<Integer> serverPorts = new CopyOnWriteArrayList<>();

    private static AtomicInteger counter = new AtomicInteger(0);

    private AtomicInteger clientCounter = new AtomicInteger(0);

    public static final int BASE_PORT = 5000;

    private KeyPair rsaKeyPair;

    public DirectoryServer() {
        try {
            System.out.println(Color.securityMessage("[Server Discovery] Gerando chaves RSA..."));
            this.rsaKeyPair = KeyGeneratorUtil.gerarParRSA();
        } catch (Exception e) {
            System.out.println(Color.securityMessage("Erro fatal ao gerar chaves RSA: " + e.getMessage()));
            System.exit(1);
        }
        this.run();
    }

    private void run() {
        try {
            // Cria o socket do servidor na porta BASE_PORT
            ServerSocket serverSocket = new ServerSocket(BASE_PORT);

            System.out.println("Servidor de diretório rodando na porta: " + BASE_PORT);
            while (true) {
                // Aguarda por conexões de clientes
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nova conexão recebida de: " + clientSocket.getInetAddress().getHostAddress());

                // Cria uma nova thread para lidar com o cliente
                ThreadDirectory handler = new ThreadDirectory(clientSocket, serverPorts, counter, clientCounter, rsaKeyPair);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            System.err.println("Erro no servidor de diretório: " + e.getMessage());
        }
    }

}
