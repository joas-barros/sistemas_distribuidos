package edu.unidade2.servicediscovery.server;

import edu.unidade2.servicediscovery.entities.Calculation;

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
            entrada = new ObjectInputStream(client.getInputStream());
            Calculation calc = (Calculation) entrada.readObject();
            System.out.println("Cliente " + calc.getClientId() + " conectado. pelo endereço: " + client.getInetAddress().getHostAddress());
            System.out.println("Cálculo recebido: " + calc.getOperation() + " " + calc.getArg1() + " " + calc.getArg2());

            String result = hancleCalculation(calc);

            out = new PrintStream(client.getOutputStream());
            out.println(result);
            System.out.println("mensagem enviada ao cliente: " + result);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (entrada != null) entrada.close();
                if (out != null) out.close();
                if (client != null) client.close();
            } catch (Exception e) {
                e.printStackTrace();
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
