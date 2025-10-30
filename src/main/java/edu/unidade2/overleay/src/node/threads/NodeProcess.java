package edu.unidade2.overleay.src.node.threads;

import edu.unidade2.overleay.src.message.MessageEntity;
import edu.unidade2.overleay.src.message.MessageType;

import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NodeProcess implements Runnable {

    int id;
    int port;
    int previousNodePort;
    int successorNodePort;
    List<String> files;

    List<String> logMessages = Collections.synchronizedList(new ArrayList<>());

    private ServerSocket serverSocket;

    public NodeProcess(int id, int port, int previousNodePort, int successorNodePort, List<String> files) {
        this.id = id;
        this.port = port;
        this.previousNodePort = previousNodePort;
        this.successorNodePort = successorNodePort;
        this.files = files;
    }

    private boolean isResponsable(String fileName) {
        return files.contains(fileName);
    }


    private void log(String message) {
        String logMsg = "Processo P" + id + ": " + message;
        logMessages.add(logMsg + "\n");
        System.out.println(logMsg);
    }

    public void startSearch(String fileName) {
        log("Iniciando busca pelo arquivo: " + fileName);

        if (isResponsable(fileName)) {
            log("Arquivo '" + fileName + "' encontrado localmente. Busca concluída.");
            return;
        }

        MessageEntity searchMessage = new MessageEntity(
                MessageType.SEARCH,
                this.id,
                null, // Destino final é desconhecido
                fileName
        );

        sendMessageToSuccessor(searchMessage);
    }

    private void sendMessageToSuccessor(MessageEntity message) {
        log("Enviando mensagem para o sucessor (porta " + successorNodePort + ")");
        try (
                Socket socket = new Socket("localhost", successorNodePort);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())
        ) {
            out.writeObject(message);
            out.flush();
        } catch (Exception e) {
            log("Erro ao enviar mensagem para processo na porta " + this.successorNodePort + ": " + e.getMessage());
        }
    }

    public void handleIncomingMessage(MessageEntity message) {
        log("Mensagem recebida de P" + message.getOriginNodeId() + ". Tipo: " + message.getType());

        switch (message.getType()) {
            case SEARCH:

                if(message.getOriginNodeId().equals(this.id)) {
                    log("Busca retornou ao nó de origem P" + this.id + ". Arquivo '" + message.getPayload() + "' não encontrado na rede.");
                    return;
                }

                String fileName = message.getPayload();
                if (isResponsable(fileName)) {
                    log("Arquivo '" + fileName + "' encontrado localmente. Enviando resposta.");
                    MessageEntity foundMessage = new MessageEntity(
                            MessageType.FOUND,
                            this.id,
                            message.getOriginNodeId(),
                            fileName
                    );
                    sendMessageToSuccessor(foundMessage);
                } else {
                    log("Arquivo '" + fileName + "' não encontrado localmente. Encaminhando busca.");
                    sendMessageToSuccessor(message);
                }
                break;
            case FOUND:
                if (message.getDestinationNodeId().equals(this.id)) {
                    log("Resposta de busca recebida: Arquivo '" + message.getPayload() + "' encontrado em P" + message.getOriginNodeId());
                } else {
                    log("Encaminhando resposta de busca para P" + message.getDestinationNodeId());
                    sendMessageToSuccessor(message);
                }
                break;
            default:
                log("Tipo de mensagem desconhecido.");
        }

    }

    public void shutdown() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            log("Erro ao fechar o socket do servidor: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        log("Servidor do nó iniciado na porta " + port);
        try {

            serverSocket = new ServerSocket(this.port);

            while (true) {
                // Bloqueia até que uma nova conexão (mensagem) chegue
                Socket clientSocket = serverSocket.accept();
                // Cria um 'Worker' para lidar com a mensagem em uma nova Thread
                // Isso atende ao requisito 'multithread' do servidor
                NodeWorker worker = new NodeWorker(clientSocket, this);
                new Thread(worker).start();
            }
        } catch (SocketException e) {
            log("Servidor encerrado.");
        }catch (Exception e) {
            log("Erro crítico no servidor do nó: " + e.getMessage());
        }
    }
}
