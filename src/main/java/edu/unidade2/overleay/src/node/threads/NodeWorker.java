package edu.unidade2.overleay.src.node.threads;

import edu.unidade2.overleay.src.message.MessageEntity;
import edu.unidade2.security.SecureMessageFacade;
import edu.unidade2.security.handler.HandshakeHandler;
import edu.unidade2.security.model.SessionKeys;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NodeWorker implements Runnable {
    private Socket clientSocket;
    private NodeProcess ownerNode;

    public NodeWorker(Socket clientSocket, NodeProcess ownerNode) {
        this.clientSocket = clientSocket;
        this.ownerNode = ownerNode;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            // Handshake seguro atuando como Servidor (usando a chave RSA do Nó)
            SessionKeys keys = HandshakeHandler.realizarComoServidor(in, out, ownerNode.getRsaKeyPair());

            // Recebe a mensagem empacotada
            String pacote = in.readLine();
            if (pacote == null) return;

            // Desempacota e valida a assinatura
            String mensagemTexto = SecureMessageFacade.desempacotarMensagem(pacote, keys.aes(), keys.hmac());

            // Converte de volta para MessageEntity e processa
            MessageEntity receivedMessage = ownerNode.deserializeMessage(mensagemTexto);
            ownerNode.handleIncomingMessage(receivedMessage);

        } catch (Exception e) {
            System.err.println("Node " + ownerNode.id + ": Erro ao ler mensagem: " + e.getMessage());
        }
    }
}
