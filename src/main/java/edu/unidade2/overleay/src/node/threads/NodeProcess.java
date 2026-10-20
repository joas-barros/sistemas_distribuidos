package edu.unidade2.overleay.src.node.threads;

import edu.unidade2.cdn.src.config.Color;
import edu.unidade2.overleay.src.message.MessageEntity;
import edu.unidade2.overleay.src.message.MessageType;
import edu.unidade2.security.SecureMessageFacade;
import edu.unidade2.security.handler.HandshakeHandler;
import edu.unidade2.security.model.SessionKeys;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NodeProcess implements Runnable {

    int id;
    int port;
    int previousNodePort;
    int successorNodePort;
    List<String> files;
    private KeyPair rsaKeyPair;

    // Usado para armazenar logs de forma thread-safe
    List<String> logMessages = Collections.synchronizedList(new ArrayList<>());

    private ServerSocket serverSocket;

    private boolean attackerMode;

    public NodeProcess(int id, int port, int previousNodePort, int successorNodePort, List<String> files, KeyPair rsaKeyPair, boolean attackerMode) {
        this.id = id;
        this.port = port;
        this.previousNodePort = previousNodePort;
        this.successorNodePort = successorNodePort;
        this.files = files;
        this.rsaKeyPair = rsaKeyPair;
        this.attackerMode = attackerMode;
    }

    private boolean isResponsable(String fileName) {
        return files.contains(fileName);
    }

    public KeyPair getRsaKeyPair() {
        return this.rsaKeyPair;
    }


    private void log(String message) {
        String logMsg = "Processo P" + id + ": " + message;
        logMessages.add(logMsg + "\n");
        System.out.println(logMsg);
    }

    public String serializeMessage(MessageEntity msg) {
        // Formato: TIPO;ORIGEM;DESTINO;PAYLOAD
        String dest = msg.getDestinationNodeId() != null ? String.valueOf(msg.getDestinationNodeId()) : "NULL";
        return msg.getType().name() + ";" + msg.getOriginNodeId() + ";" + dest + ";" + msg.getPayload();
    }

    public MessageEntity deserializeMessage(String text) {
        String[] partes = text.split(";", 4);
        MessageType type = MessageType.valueOf(partes[0]);
        Integer origin = Integer.parseInt(partes[1]);
        Integer dest = partes[2].equals("NULL") ? null : Integer.parseInt(partes[2]);
        String payload = partes[3];
        return new MessageEntity(type, origin, dest, payload);
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
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            SessionKeys keys = HandshakeHandler.realizarComoCliente(in, out);
            log(Color.securityMessage("Handshake seguro realizado com sucesso na porta " + successorNodePort));

            String plainMsg = serializeMessage(message);
            String secureMsg = SecureMessageFacade.empacotarMensagem(plainMsg, keys.aes(), keys.hmac());

            if (attackerMode) {
                secureMsg = SecureMessageFacade.empacotarMensagem("MENSAGEM ALTERADA PELO ATACANTE", keys.aes(), keys.hmac());
                log(Color.securityMessage("Modo atacante ativo: Mensagem alterada antes de enviar para o sucessor."));
            }

            log(Color.securityMessage("Mensagem " + plainMsg + " empacotada como " + secureMsg + " e enviada para o sucessor na porta " + successorNodePort));

            out.println(secureMsg);
            out.flush();
        } catch (SecurityException se) {
            log(Color.securityMessage("Falha de segurança ao contactar sucessor: " + se.getMessage()));
        } catch (Exception e) {
            log("Erro ao enviar mensagem para processo na porta " + this.successorNodePort + ": " + e.getMessage());
        }
    }

    public void handleIncomingMessage(MessageEntity message) {
        log("Mensagem recebida de P" + message.getOriginNodeId() + ". Tipo: " + message.getType());

        switch (message.getType()) {
            case SEARCH:

                // Indicativo de que o arquivo não foi encontrado na rede
                if(message.getOriginNodeId().equals(this.id)) {
                    log("Busca retornou ao nó de origem P" + this.id + ". Arquivo '" + message.getPayload() + "' não encontrado na rede.");
                    return;
                }

                // Indica que o arquivo foi encontrado localment
                String fileName = message.getPayload();
                if (isResponsable(fileName)) {
                    log("Arquivo '" + fileName + "' encontrado localmente. Enviando resposta.");

                    // Muda a mensagem para FOUND e define o destino como o nó de origem
                    MessageEntity foundMessage = new MessageEntity(
                            MessageType.FOUND,
                            this.id,
                            message.getOriginNodeId(),
                            fileName
                    );

                    // Envia a mensagem de volta para o nó de origem
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

    // Força uma exception no while(true) do servidor para encerrá-lo
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
