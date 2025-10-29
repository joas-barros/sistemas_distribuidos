package edu.unidade2.overleay.src.node.threads;

import edu.unidade2.overleay.src.message.MessageEntity;

import java.io.ObjectInputStream;
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
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())
        ) {
            MessageEntity receivedMessage = (MessageEntity) in.readObject();

            ownerNode.handleIncomingMessage(receivedMessage);

        } catch (Exception e) {
            System.err.println("Node " + ownerNode.id + ": Erro ao ler mensagem: " + e.getMessage());
        }
    }
}
