package edu.unidade2.servicediscovery.client;

import edu.unidade2.servicediscovery.entities.Calculation;
import edu.unidade2.servicediscovery.entities.Operation;

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

            out = new PrintStream(client.getOutputStream());
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            keyboard = new Scanner(System.in);

            String message = "GET PORT";

            out.println(message);
            System.out.println("Mensagem enviada ao servidor diretorio: " + message);

            String recievedMessage = in.readLine();
            System.out.println("Mensagem recebida do servidor diretorio: " + recievedMessage);

            int port = Integer.parseInt(recievedMessage.split(" ")[1]);

            System.out.println("Porta recebida: " + port);

            System.out.println("Encerrando conexao com o servidor diretorio...");
            client.close();

            System.out.println("Conectando ao servidor de calculos na porta " + port + "...");
            calcSocket = new Socket(client.getInetAddress(), port);

            System.out.println("Escolha a operação que deseja realizar (Operação inválida irá encerrar o programa): ");
            System.out.println("1 - Adição");
            System.out.println("2 - Subtração");
            System.out.println("3 - Multiplicação");
            System.out.println("4 - Divisão");

            int number = keyboard.nextInt();
            keyboard.nextLine();

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
                System.out.println("Operação inválida. Encerrando o programa.");
                return;
            }

            System.out.println("Digite o primeiro número: ");
            double arg1 = keyboard.nextDouble();
            keyboard.nextLine();
            System.out.println("Digite o segundo número: ");
            double arg2 = keyboard.nextDouble();
            keyboard.nextLine();

            Calculation calculation = new Calculation(operation, arg1, arg2);
            outObject = new ObjectOutputStream(calcSocket.getOutputStream());
            outObject.writeObject(calculation);
            outObject.flush();

            recievedMessage = in.readLine();
            System.out.println("Resultado recebido do servidor de cálculos: " + recievedMessage);

            System.out.println("Encerrando conexão com o servidor de cálculos...");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (calcSocket != null && !calcSocket.isClosed()) {
                    calcSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                keyboard.close();
            }
        }


    }
}
