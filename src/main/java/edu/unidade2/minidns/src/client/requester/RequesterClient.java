package edu.unidade2.minidns.src.client.requester;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class RequesterClient {
    DatagramSocket socketCliente = null;
    Scanner sc = new Scanner(System.in);
    InetAddress adress;
    private final String SERVER_HOST = "localhost";
    private final int SERVER_PORT = 12345;

    public RequesterClient() {
        this.run();
    }

    private void run() {
        int choice;
        try{
            socketCliente = new DatagramSocket();
            adress = InetAddress.getByName(SERVER_HOST);

            System.out.println("Cliente requisitante rodando em: " +
                    InetAddress.getLocalHost() + ":" +
                    socketCliente.getLocalPort());

            boolean running = true;
            while (running) {
                System.out.println("\n--- Requisição de Servidor ---");
                System.out.println("[1] - Consultar o endereço IP de um servidor");
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
                        System.out.println("Cliente Requisitante encerrado.");
                        continue;
                    default:
                        System.out.println("Opção inválida. Tente novamente.");
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleServerRequest() throws IOException {
        // Solicitar o nome do servidor ao usuário
        System.out.println("Digite o nome do servidor a ser consultado: ");
        String serverName = sc.nextLine();

        // Construir a mensagem de consulta
        String message = "GET:" + serverName;
        byte[] sendData = message.getBytes();

        // Enviar a mensagem ao servidor
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                adress, SERVER_PORT);
        socketCliente.send(sendPacket);
        System.out.println("Mensagem de consulta enviada: " + message);

        // Receber a resposta do servidor
        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socketCliente.receive(receivePacket);
        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
        System.out.println("Resposta recebida: " + response);
    }
}
