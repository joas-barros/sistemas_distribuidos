package edu.unidade2.overleay.node;

import java.util.ArrayList;
import java.util.List;

public class NodeProcess implements Runnable {

    int id;
    int port;
    int previousNodePort;
    int successorNodePort;
    List<String> files;
    List<String> logMessages = new ArrayList<>();

    public NodeProcess(int id, int port, int previousNodePort, int successorNodePort, List<String> files) {
        this.id = id;
        this.port = port;
        this.previousNodePort = previousNodePort;
        this.successorNodePort = successorNodePort;
        this.files = files;
    }

    private boolean isResponsableForFile(String fileName) {
        int fileHash = Math.abs(fileName.hashCode() % 256);
        int previousNodeHash = Math.abs(Integer.hashCode(previousNodePort) % 256);
        int currentNodeHash = Math.abs(Integer.hashCode(port) % 256);

        if (previousNodeHash < currentNodeHash) {
            return fileHash > previousNodeHash && fileHash <= currentNodeHash;
        } else {
            return fileHash > previousNodeHash || fileHash <= currentNodeHash;
        }
    }


    private void log(String message) {
        logMessages.add(message);
        System.out.println("Node " + id + ": " + message);
    }

    private void sendMessageToSuccessor(String message) {
        // Placeholder for sending message to successor node
        log("Sending message to successor (" + successorNodePort + "): " + message);
    }

    public void initialize() {
        log("Node initialized on port " + port + " with files: " + files);
    }

    @Override
    public void run() {

    }
}
