package edu.unidade2.overleay.src.node;

import edu.unidade2.overleay.src.node.threads.NodeProcess;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Node {
    int id;
    int port;
    int successorPort;
    Integer previousPort;
    Integer initialFile;
    int lastFile;

    public Node(int id, int port, int successorPort, Integer previousPort, int initialFile, int lastFile) {
        this.id = id;
        this.port = port;
        this.successorPort = successorPort;
        this.previousPort = previousPort;
        this.initialFile = initialFile;
        this.lastFile = lastFile;
        this.run();
    }

    private void run() {
        NodeProcess process = new NodeProcess(
                id,
                port,
                previousPort,
                successorPort,
                generateFiles()
        );

        Thread thread = new Thread(process);
        thread.start();

        System.out.println("--- PROCESSO P" + id + " PRONTO ---");
        System.out.println("Digite o nome do arquivo para buscar (ex: arquivo42) ou 'sair'");

        Scanner keyboard = new Scanner(System.in);

        boolean exit = false;
        while (!exit) {
            String input = keyboard.nextLine();
            if (input.equalsIgnoreCase("sair")) {
                System.out.println("Encerrando processo P" + id + "...");
                process.shutdown();
                exit = true;
            } else if (input.trim().isEmpty() || !input.startsWith("arquivo")){
                System.out.println("Nome de arquivo inválido. Tente novamente.");
            } else {
                process.startSearch(input);
            }
        }

        keyboard.close();
    }

    private List<String> generateFiles() {
        List<String> files = new ArrayList<>();
        for (int i = initialFile; i <= lastFile; i++) {
            files.add("arquivo" + i);
        }
        return files;
    }

}
