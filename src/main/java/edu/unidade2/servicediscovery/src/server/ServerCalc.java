package edu.unidade2.servicediscovery.src.server;

import edu.unidade2.servicediscovery.src.server.proxy.DirectoryServer;

import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerCalc {

    ServerSocket serverSocket;
    Socket client;
    int porta;

    public ServerCalc(int porta) {
        this.porta = porta;
        this.rodar();
    }

    private void rodar() {
        try {
            serverSocket = new ServerSocket(porta);
            System.out.println("Servidor de calculo rodando na porta: " + porta);

            Socket directorySocket = new Socket("localhost", DirectoryServer.BASE_PORT);
            System.out.println("Conectado ao servidor diretorio para registro de porta.");

            PrintStream out = new PrintStream(directorySocket.getOutputStream());
            String registerMessage = "REGISTER " + porta;
            out.println(registerMessage);
            System.out.println("Mensagem enviada ao servidor diretorio: " + registerMessage);

            System.out.println("Encerrando conexao com o servidor diretorio...");
            directorySocket.close();

            while (true) {
                client = serverSocket.accept();
                System.out.println("Cliente conectado: " + client.getInetAddress().getHostAddress());

                ThreadServerCalc threadServerCalc = new ThreadServerCalc(client);
                Thread thread = new Thread(threadServerCalc);
                thread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
