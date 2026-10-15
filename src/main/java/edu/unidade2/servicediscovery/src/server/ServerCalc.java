package edu.unidade2.servicediscovery.src.server;

import edu.unidade2.cdn.src.config.Color;
import edu.unidade2.security.SecureMessageFacade;
import edu.unidade2.security.handler.HandshakeHandler;
import edu.unidade2.security.keys.KeyGeneratorUtil;
import edu.unidade2.security.model.SessionKeys;
import edu.unidade2.servicediscovery.src.server.proxy.DirectoryServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;

public class ServerCalc {

    ServerSocket serverSocket;
    Socket client;
    int porta;
    private KeyPair rsaKeyPair;

    public ServerCalc(int porta) {
        this.porta = porta;

        try {
            System.out.println(Color.securityMessage("[Server Calc] Gerando chaves RSA..."));
            this.rsaKeyPair = KeyGeneratorUtil.gerarParRSA();
        } catch (Exception e) {
            System.out.println(Color.securityMessage("Erro fatal ao gerar chaves RSA: " + e.getMessage()));
            System.exit(1);
        }

        this.rodar();
    }

    private void rodar() {
        registrarNoDiretorio();
        escutarClientes();
    }

    private void registrarNoDiretorio() {
        System.out.println("Conectando ao servidor diretorio para registro de porta...");
        try (
                Socket directorySocket = new Socket("localhost", DirectoryServer.BASE_PORT);
                BufferedReader inDir = new BufferedReader(new InputStreamReader(directorySocket.getInputStream()));
                PrintWriter outDir = new PrintWriter(directorySocket.getOutputStream(), true)
        ) {
            // 1. Handshake atuando como Cliente perante o Diretório
            SessionKeys keysDir = HandshakeHandler.realizarComoCliente(inDir, outDir);

            // 2. Empacota e envia o comando de registro
            String registerMessage = "REGISTER " + porta;
            String pacoteRegistro = SecureMessageFacade.empacotarMensagem(registerMessage, keysDir.aes(), keysDir.hmac());
            outDir.println(pacoteRegistro);

            System.out.println("Mensagem segura enviada ao servidor diretorio: " + registerMessage);

            // 3. Aguarda a confirmação (REGISTER_OK) do diretório
            String respostaCifrada = inDir.readLine();
            if (respostaCifrada != null) {
                String resposta = SecureMessageFacade.desempacotarMensagem(respostaCifrada, keysDir.aes(), keysDir.hmac());
                System.out.println("Confirmação do diretório: " + resposta);
            }
            System.out.println("Encerrando conexao com o servidor diretorio...");
        } catch (SecurityException se) {
            System.err.println("[SECURITY ALERT] Falha de integridade ao se registrar no diretório: " + se.getMessage());
        } catch (Exception e) {
            System.err.println("Erro ao registrar de forma segura no diretório: " + e.getMessage());
        }
    }

    private void escutarClientes() {
        try {
            serverSocket = new ServerSocket(porta);
            System.out.println("Servidor de calculo rodando na porta: " + porta);

            while (true) {
                client = serverSocket.accept();
                System.out.println("Cliente conectado: " + client.getInetAddress().getHostAddress());

                ThreadServerCalc threadServerCalc = new ThreadServerCalc(client, rsaKeyPair);
                Thread thread = new Thread(threadServerCalc);
                thread.start();
            }
        } catch (Exception e) {
            System.err.println("Erro no servidor de calculo: " + e.getMessage());
        }
    }

}
