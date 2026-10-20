package edu.unidade2.minidns.src.client.register;

import edu.unidade2.cdn.src.config.Color;
import edu.unidade2.security.handler.UdpSecurityHandler;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.PublicKey;
import java.util.Scanner;

public class RegisterClient {
    DatagramSocket socketCliente = null;
    InetAddress adress;
    Scanner sc = new Scanner(System.in);
    private final String SERVER_HOST = "localhost";
    private final int SERVER_PORT = 12345;

    public RegisterClient() {
        this.run();
    }

    private void run() {
        int choice;
        try{
            socketCliente = new DatagramSocket();
            adress = InetAddress.getByName(SERVER_HOST);

            System.out.println("Cliente de Registrador MiniDNS iniciado em: " +
                    InetAddress.getLocalHost() + ":" +
                    socketCliente.getLocalPort());


            boolean running = true;
            while (running) {
                System.out.println("\n--- Registro de Servidor ---");
                System.out.println("[1] - Atualizar o endereço IP de um servidor");
                System.out.println("[0] - Sair");
                System.out.print("Escolha uma opção: ");
                String line = sc.nextLine();

                try {
                    choice = Integer.parseInt(line);
                } catch (NumberFormatException e) {
                    System.out.println("Entrada inválida. Por favor, insira um número.");
                    continue;
                }

                switch (choice) {
                    case 1:
                        handleServerRequest();
                        continue;
                    case 0:
                        running = false;
                        System.out.println("Cliente Registrador encerrado.");
                        continue;
                    default:
                        System.out.println("Opção inválida. Tente novamente.");
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            sc.close();
            socketCliente.close();
        }
    }

    private void handleServerRequest() {
        System.out.print("Digite o nome do servidor a ser registrado: ");
        String serverName = sc.nextLine();
        System.out.print("Digite o endereço IP do servidor: ");
        String serverIP = sc.nextLine();

        String registrationMessage = "UPDATE:" + serverName + ":" + serverIP;

        try {
            System.out.println(Color.infoMessage("[Cliente] Solicitando chave pública do MiniDNS..."));
            PublicKey pubKey = UdpSecurityHandler.solicitarChavePublica(socketCliente, adress, SERVER_PORT);

            System.out.println(Color.infoMessage("[Cliente] Enviando requisição cifrada e assinada (HMAC)..."));
            String response = UdpSecurityHandler.enviarEReceberSeguro(socketCliente, adress, SERVER_PORT, registrationMessage, pubKey);

            System.out.println(Color.successMessage("Resposta Segura do servidor: " + response));
        } catch (SecurityException se) {
            System.out.println(Color.securityMessage("A comunicação foi abortada por falha de segurança/integridade."));
        } catch (Exception e) {
            System.out.println(Color.errorMessage("Erro na comunicação UDP: " + e.getMessage()));
        }
    }
}
