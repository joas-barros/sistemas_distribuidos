package edu.unidade2.servicediscovery.src.server;

import edu.unidade2.servicediscovery.src.server.proxy.DirectoryServer;

import java.io.PrintStream;
import java.net.ConnectException;
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
            // Cria o socket do servidor na porta especificada
            serverSocket = new ServerSocket(porta);
            System.out.println("Servidor de calculo rodando na porta: " + porta);

            // Conecta ao servidor diretorio para solicitar o registro da porta
            Socket directorySocket = new Socket("localhost", DirectoryServer.BASE_PORT);
            System.out.println("Conectado ao servidor diretorio para registro de porta.");

            // Envia a mensagem de registro
            PrintStream out = new PrintStream(directorySocket.getOutputStream());
            String registerMessage = "REGISTER " + porta;
            out.println(registerMessage);
            System.out.println("Mensagem enviada ao servidor diretorio: " + registerMessage);

            // Após se registrar, fecha a conexão com o servidor diretorio
            System.out.println("Encerrando conexao com o servidor diretorio...");
            directorySocket.close();

            // Aguarda por conexões de clientes
            while (true) {
                client = serverSocket.accept();
                System.out.println("Cliente conectado: " + client.getInetAddress().getHostAddress());

                // cria uma nova thread assim que um cliente se conecta
                ThreadServerCalc threadServerCalc = new ThreadServerCalc(client);
                Thread thread = new Thread(threadServerCalc);
                thread.start();
            }
        } catch (ConnectException e) {
            System.err.println(e.getMessage());
        }
        catch (Exception e) {
            System.err.println("Erro no servidor de calculo: " + e.getMessage());
        }
    }

}
