package edu.unidade2.servicediscovery.src.server;

import edu.unidade2.servicediscovery.src.entities.Calculation;

import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.net.Socket;

public class ThreadServerCalc implements Runnable{

    private Socket client;
    private ObjectInputStream entrada;
    private PrintStream out;

    public ThreadServerCalc(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            // recebe o objeto Calculation do cliente
            entrada = new ObjectInputStream(client.getInputStream());
            Calculation calc = (Calculation) entrada.readObject();

            // exibe as informações do cálculo recebido
            System.out.println("Cliente " + calc.getClientId() + " conectado. pelo endereço: " + client.getInetAddress().getHostAddress());
            System.out.println("Cálculo recebido: " + calc.getOperation() + " " + calc.getArg1() + " " + calc.getArg2());

            // realiza o cálculo e obtém o resultado
            String result = hancleCalculation(calc);

            // envia o resultado de volta ao cliente
            out = new PrintStream(client.getOutputStream());
            out.println(result);
            System.out.println("mensagem enviada ao cliente: " + result);

        } catch (Exception e) {
            System.err.println("Erro na thread do servidor de calculo: " + e.getMessage());
        } finally {
            try {
                if (entrada != null) entrada.close();
                if (out != null) out.close();
                if (client != null) client.close();
            } catch (Exception e) {
                System.err.println("Erro ao fechar recursos na thread do servidor de calculo: " + e.getMessage());
            }
        }
    }

    private String hancleCalculation(Calculation calc) {
        switch (calc.getOperation()) {
            case ADICAO:
                return  "Resultado: " + calc.getArg1() + " + " + calc.getArg2() + " = " + (calc.getArg1() + calc.getArg2());
            case SUBTRACAO:
                return  "Resultado: " + calc.getArg1() + " - " + calc.getArg2() + " = " + (calc.getArg1() - calc.getArg2());
            case MULTIPLICACAO:
                return  "Resultado: " + calc.getArg1() + " * " + calc.getArg2() + " = " + (calc.getArg1() * calc.getArg2());
            case DIVISAO:
                if (calc.getArg2() != 0) {
                    return  "Resultado: " + calc.getArg1() + " / " + calc.getArg2() + " = " + (calc.getArg1() / calc.getArg2());
                } else {
                    return "Erro: Divisão por zero.";
                }
            default:
                return "Operação inválida.";
        }
    }
}
