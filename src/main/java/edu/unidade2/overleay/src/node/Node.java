package edu.unidade2.overleay.src.node;

import edu.unidade2.cdn.src.config.Color;
import edu.unidade2.overleay.src.node.threads.NodeProcess;
import edu.unidade2.security.keys.KeyGeneratorUtil;

import java.security.KeyPair;
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
    boolean attackerMode;

    private KeyPair rsaKeyPair;

    public Node(int id, int port, int successorPort, Integer previousPort, int initialFile, int lastFile, boolean attackerMode) {
        this.id = id;
        this.port = port;
        this.successorPort = successorPort;
        this.previousPort = previousPort;
        this.initialFile = initialFile;
        this.lastFile = lastFile;
        this.attackerMode = attackerMode;

        try {
            System.out.println(Color.securityMessage("[P" + id  + "]" + " Gerando chaves RSA..."));
            this.rsaKeyPair = KeyGeneratorUtil.gerarParRSA();
        } catch (Exception e) {
            System.out.println(Color.securityMessage("Erro fatal ao gerar chaves RSA: " + e.getMessage()));
            System.exit(1);
        }

        this.run();
    }

    public KeyPair getRsaKeyPair() {
        return this.rsaKeyPair;
    }

    private void run() {

        // Inicia o processo do nó em uma nova thread
        NodeProcess process = new NodeProcess(
                id,
                port,
                previousPort,
                successorPort,
                generateFiles(),
                rsaKeyPair,
                attackerMode
        );

        // Inicia o processo em uma nova thread
        Thread thread = new Thread(process);
        thread.start();

        System.out.println("--- PROCESSO P" + id + " PRONTO ---");
        System.out.println("Digite o nome do arquivo para buscar (ex: arquivo42) ou 'sair'");

        Scanner keyboard = new Scanner(System.in);

        // Loop principal para ler comandos do usuário
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

    // Gera a lista de arquivos que este nó é responsável por armazenar
    private List<String> generateFiles() {
        List<String> files = new ArrayList<>();
        for (int i = initialFile; i <= lastFile; i++) {
            files.add("arquivo" + i);
        }
        return files;
    }

}
