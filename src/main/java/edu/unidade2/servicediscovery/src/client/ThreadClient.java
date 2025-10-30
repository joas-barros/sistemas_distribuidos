package edu.unidade2.servicediscovery.src.client;

import edu.unidade2.servicediscovery.src.entities.Calculation;
import edu.unidade2.servicediscovery.src.entities.Operation;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ThreadClient implements Runnable{

    private Socket client;

    private PrintStream out;
    private BufferedReader in;
    private ObjectOutputStream outObject;
    private Scanner keyboard;
    private Socket calcSocket;

    public ThreadClient(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {

        System.out.println("Thread do cliente rodando...");

        try {

            // Configura os streams de entrada e saída
            out = new PrintStream(client.getOutputStream());
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            keyboard = new Scanner(System.in);

            // prepara a requisição para o servidor diretorio
            String message = "GET PORT";

            // envia a requisição ao servidor diretorio
            out.println(message);
            System.out.println("Mensagem enviada ao servidor diretorio: " + message);

            // aguarda a resposta do servidor diretorio
            String recievedMessage = in.readLine();
            System.out.println("Mensagem recebida do servidor diretorio: " + recievedMessage);

            // lidar com a resposta do servidor diretorio que não há servidores disponíveis
            if(recievedMessage.equals("NO SERVERS AVAILABLE")){
                System.err.println("Nenhum servidor de cálculo disponível. Encerrando o programa.");
                return;
            }

            // extrai a porta e o id do cliente da mensagem recebida
            int port = Integer.parseInt(recievedMessage.split(" ")[1]);
            int id = Integer.parseInt(recievedMessage.split(" ")[3]);

            // exibe a porta e o id recebidos
            System.out.println("Porta recebida: " + port);
            System.out.println("Seu ID de cliente é: " + id);

            // fecha a conexão com o servidor diretorio
            System.out.println("Encerrando conexao com o servidor diretorio...");
            client.close();

            // conecta ao servidor de calculos na porta recebida
            System.out.println("Conectando ao servidor de calculos na porta " + port + "...");
            calcSocket = new Socket(client.getInetAddress(), port);

            // solicita ao usuário a operação e os números para o cálculo
            System.out.println("Escolha a operação que deseja realizar (Operação inválida irá encerrar o programa): ");
            System.out.println("1 - Adição");
            System.out.println("2 - Subtração");
            System.out.println("3 - Multiplicação");
            System.out.println("4 - Divisão");

            String line = keyboard.nextLine();
            int number;

            // lidando com entradas não numericas
            try {
                number = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.err.println("Entrada inválida. Encerrando o programa.");
                return;
            }

            Operation operation = null;

            switch (number) {
                case 1:
                    operation = Operation.ADICAO;
                    break;
                case 2:
                    operation = Operation.SUBTRACAO;
                    break;
                case 3:
                    operation = Operation.MULTIPLICACAO;
                    break;
                case 4:
                    operation = Operation.DIVISAO;
                    break;
                default:
                    operation = Operation.NOT_VALID;
                    break;
            }

            if (operation == Operation.NOT_VALID) {
                System.err.println("Operação inválida. Encerrando o programa.");
                return;
            }

            // solicita os números ao usuário
            System.out.println("Digite o primeiro número: ");
            double arg1 = keyboard.nextDouble();
            keyboard.nextLine();
            System.out.println("Digite o segundo número: ");
            double arg2 = keyboard.nextDouble();
            keyboard.nextLine();

            // cria o objeto Calculation e envia ao servidor de cálculos
            Calculation calculation = new Calculation(operation, arg1, arg2, id);
            outObject = new ObjectOutputStream(calcSocket.getOutputStream());
            outObject.writeObject(calculation);
            outObject.flush();

            // aguarda a resposta do servidor de cálculos
            in = new BufferedReader(new InputStreamReader(calcSocket.getInputStream()));

            // Exibe o resultado recebido do servidor de cálculos
            recievedMessage = in.readLine();
            System.out.println("Resultado recebido do servidor de cálculos: " + recievedMessage);

            System.out.println("Encerrando conexão com o servidor de cálculos...");
        } catch (IOException e) {
            System.err.println("Erro de I/O: " + e.getMessage());
        } finally {
            try {
                if (calcSocket != null && !calcSocket.isClosed()) {
                    calcSocket.close();
                }
            } catch (IOException e) {
                System.err.println("Erro ao fechar o socket de cálculo: " + e.getMessage());
            } finally {
                keyboard.close();
            }
        }


    }
}
