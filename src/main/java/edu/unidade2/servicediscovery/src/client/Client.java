package edu.unidade2.servicediscovery.src.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class Client {

    Socket socket;
    InetAddress inet;
    String ip;
    int porta;

    public Client(String ip, int porta) {
        this.ip = ip;
        this.porta = porta;
        this.run();
    }

    private void run() {
        try {
            inet = InetAddress.getByName(ip);
            socket = new Socket(inet, porta);
            System.out.println("Conectado ao servidor de diretorio: " + socket.getInetAddress().getHostAddress());

            ThreadClient threadClient = new ThreadClient(socket);

            Thread thread = new Thread(threadClient);
            thread.start();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
