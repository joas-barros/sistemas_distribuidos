package edu.unidade2.minidns.client.register;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Scanner;

public class ImplRegisterClient {
    DatagramSocket socketCliente = null;
    Scanner sc = new Scanner(System.in);

    public ImplRegisterClient() {
        this.run();
    }

    private void run() {
        int choice;
        try{
            socketCliente = new DatagramSocket();
            System.out.println("Cliente de Registro MiniDNS iniciado.");

            boolean running = true;
            while (running) {
                System.out.println("\n--- Registro de Servidor ---");
                System.out.println("[1] - Atualizar o endereço IP de um servidor");
                System.out.println("[0] - Sair");
                System.out.print("Escolha uma opção: ");
                choice = sc.nextInt();

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
                        continue;
                }


            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleServerRequest() throws IOException {
        System.out.print("Digite o nome do servidor a ser registrado: ");
        String serverName = sc.nextLine();
        System.out.print("Digite o endereço IP do servidor: ");
        String serverIP = sc.nextLine();

        String registrationMessage = "UPDATE:" + serverName + ":" + serverIP;
        byte[] sendData = registrationMessage.getBytes();

        // Enviar a mensagem de registro para o servidor MiniDNS
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                java.net.InetAddress.getByName("localhost"), 12345);
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
