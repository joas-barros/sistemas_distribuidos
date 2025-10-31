package edu.unidade2.minidns.src.client.register;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
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

    private void handleServerRequest() throws IOException {
        // Solicitar o nome do servidor e o novo endereço IP ao usuário
        System.out.print("Digite o nome do servidor a ser registrado: ");
        String serverName = sc.nextLine();
        System.out.print("Digite o endereço IP do servidor: ");
        String serverIP = sc.nextLine();

        // Construir a mensagem de registro
        String registrationMessage = "UPDATE:" + serverName + ":" + serverIP;
        byte[] sendData = registrationMessage.getBytes();

        // Enviar a mensagem de registro para o servidor MiniDNS
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                adress, SERVER_PORT);
        socketCliente.send(sendPacket);
        System.out.println("Mensagem de registro enviada: " + registrationMessage);

        // Receber a resposta do servidor
        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socketCliente.receive(receivePacket);
        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
        System.out.println("Resposta do servidor: " + response);
    }
}
