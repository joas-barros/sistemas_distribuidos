package edu.unidade2.servicediscovery.src.client;

import edu.unidade2.security.SecureMessageFacade;
import edu.unidade2.security.handler.HandshakeHandler;
import edu.unidade2.security.model.SessionKeys;
import edu.unidade2.servicediscovery.src.entities.Calculation;
import edu.unidade2.servicediscovery.src.entities.Operation;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ThreadClient implements Runnable {

    private Socket client;
    private Socket calcSocket;

    private record DirectoryResponse(int port, int clientId) {}

    public ThreadClient(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        System.out.println("Thread do cliente rodando...");
        Scanner keyboard = new Scanner(System.in);

        try {
            DirectoryResponse dirInfo = fetchServiceFromDirectory(client);
            if (dirInfo == null) return;

            Calculation calculation = collectUserCalculation(keyboard, dirInfo.clientId());
            if (calculation == null) return;

            String calcString = serializeCalculation(calculation);
            executeSecureCalculation(client.getInetAddress().getHostAddress(), dirInfo.port(), calcString);

        } catch (SecurityException se) {
            System.err.println("[SECURITY ALERT] Falha de segurança/integridade na comunicação: " + se.getMessage());
        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
        } finally {
            keyboard.close();
        }
    }

    private DirectoryResponse fetchServiceFromDirectory(Socket dirSocket) throws Exception {
        try (
                BufferedReader inDir = new BufferedReader(new InputStreamReader(dirSocket.getInputStream()));
                PrintWriter outDir = new PrintWriter(dirSocket.getOutputStream(), true)
        ) {
            SessionKeys keysDir = HandshakeHandler.realizarComoCliente(inDir, outDir);
            outDir.println(SecureMessageFacade.empacotarMensagem("GET PORT", keysDir.aes(), keysDir.hmac()));

            String dirResponseEnc = inDir.readLine();
            String recievedMessage = SecureMessageFacade.desempacotarMensagem(dirResponseEnc, keysDir.aes(), keysDir.hmac());

            if (recievedMessage.equals("NO SERVERS AVAILABLE")) {
                System.err.println("Nenhum servidor de cálculo disponível. Encerrando o programa.");
                return null;
            }

            int port = Integer.parseInt(recievedMessage.split(" ")[1]);
            int id = Integer.parseInt(recievedMessage.split(" ")[3]);

            System.out.println("Porta recebida: " + port);
            System.out.println("Seu ID de cliente é: " + id);
            return new DirectoryResponse(port, id);
        } finally {
            if (dirSocket != null && !dirSocket.isClosed()) {
                dirSocket.close();
            }
        }
    }

    private Calculation collectUserCalculation(Scanner keyboard, int clientId) {
        System.out.println("Escolha a operação que deseja realizar:");
        System.out.println("1 - Adição\n2 - Subtração\n3 - Multiplicação\n4 - Divisão");

        String line = keyboard.nextLine();
        int number;
        try {
            number = Integer.parseInt(line);
        } catch (NumberFormatException e) {
            System.err.println("Entrada inválida. Encerrando o programa.");
            return null;
        }

        Operation operation = switch (number) {
            case 1 -> Operation.ADICAO;
            case 2 -> Operation.SUBTRACAO;
            case 3 -> Operation.MULTIPLICACAO;
            case 4 -> Operation.DIVISAO;
            default -> Operation.NOT_VALID;
        };

        if (operation == Operation.NOT_VALID) {
            System.err.println("Operação inválida. Encerrando o programa.");
            return null;
        }

        System.out.println("Digite o primeiro número: ");
        double arg1 = keyboard.nextDouble();
        System.out.println("Digite o segundo número: ");
        double arg2 = keyboard.nextDouble();

        return new Calculation(operation, arg1, arg2, clientId);
    }

    private void executeSecureCalculation(String serverIp, int port, String calcString) throws Exception {
        System.out.println("Conectando ao servidor de calculos na porta " + port + "...");

        try {
            calcSocket = new Socket(serverIp, port);
            BufferedReader inCalc = new BufferedReader(new InputStreamReader(calcSocket.getInputStream()));
            PrintWriter outCalc = new PrintWriter(calcSocket.getOutputStream(), true);

            SessionKeys keysCalc = HandshakeHandler.realizarComoCliente(inCalc, outCalc);
            outCalc.println(SecureMessageFacade.empacotarMensagem(calcString, keysCalc.aes(), keysCalc.hmac()));

            String encResult = inCalc.readLine();
            String finalResult = SecureMessageFacade.desempacotarMensagem(encResult, keysCalc.aes(), keysCalc.hmac());

            System.out.println("Resultado Seguro recebido: " + finalResult);
        } finally {
            if (calcSocket != null && !calcSocket.isClosed()) {
                calcSocket.close();
            }
        }
    }

    private String serializeCalculation(Calculation calc) {
        return calc.getOperation().name() + ";" + calc.getArg1() + ";" + calc.getArg2() + ";" + calc.getClientId();
    }
}
