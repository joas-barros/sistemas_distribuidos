package edu.unidade2.servicediscovery.src.server;

import edu.unidade2.cdn.src.config.Color;
import edu.unidade2.security.SecureMessageFacade;
import edu.unidade2.security.handler.HandshakeHandler;
import edu.unidade2.security.model.SessionKeys;
import edu.unidade2.servicediscovery.src.entities.Calculation;
import edu.unidade2.servicediscovery.src.entities.Operation;

import java.io.*;
import java.net.Socket;
import java.security.KeyPair;

public class ThreadServerCalc implements Runnable{

    private Socket client;
    private KeyPair rsaKeyPair;

    public ThreadServerCalc(Socket client, KeyPair rsaKeyPair) {
        this.client = client;
        this.rsaKeyPair = rsaKeyPair;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(client.getOutputStream(), true)
        ) {
            // Handshake seguro atuando como Servidor
            SessionKeys keys = HandshakeHandler.realizarComoServidor(in, out, rsaKeyPair);

            // Recebe e decifra o cálculo
            String pacoteCifrado = in.readLine();
            if (pacoteCifrado == null) return;

            System.out.println(Color.securityMessage("[Server Calc] Cálculo recebido (cifrado): " + pacoteCifrado));

            String plainCalc = SecureMessageFacade.desempacotarMensagem(pacoteCifrado, keys.aes(), keys.hmac());

            System.out.println(Color.securityMessage("[Server Calc] Cálculo recebido (decifrado): " + plainCalc));

            // Converte a String de volta para o objeto Calculation
            Calculation calc = deserializeCalculation(plainCalc);

            System.out.println("Cliente " + calc.getClientId() + " conectado pelo endereço: " + client.getInetAddress().getHostAddress());
            System.out.println("Cálculo seguro recebido: " + calc.getOperation() + " " + calc.getArg1() + " " + calc.getArg2());

            // Calcula e responde cifrado
            String result = hancleCalculation(calc);

            System.out.println("Resultado do cálculo seguro: " + result);

            String pacoteCifradoResposta = SecureMessageFacade.empacotarMensagem(result, keys.aes(), keys.hmac());

            System.out.println(Color.securityMessage("[Server Calc] Resultado enviado ao cliente (cifrado): " + pacoteCifradoResposta));
            out.println(SecureMessageFacade.empacotarMensagem(result, keys.aes(), keys.hmac()));

            System.out.println("Mensagem segura enviada ao cliente: " + result);

        } catch (SecurityException se) {
            System.out.println(Color.securityMessage("Requisição rejeitada no ServerCalc: " + se.getMessage()));
        } catch (Exception e) {
            System.err.println("Erro na thread do servidor de calculo: " + e.getMessage());
        } finally {
            try {
                if (client != null && !client.isClosed()) client.close();
            } catch (Exception e) {
                System.err.println("Erro ao fechar recursos: " + e.getMessage());
            }
        }
    }

    private Calculation deserializeCalculation(String data) {
        // Formato esperado: OPERACAO;ARG1;ARG2;CLIENT_ID
        String[] parts = data.split(";");
        Operation op = Operation.valueOf(parts[0]);
        double arg1 = Double.parseDouble(parts[1]);
        double arg2 = Double.parseDouble(parts[2]);
        int clientId = Integer.parseInt(parts[3]);
        return new Calculation(op, arg1, arg2, clientId);
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
