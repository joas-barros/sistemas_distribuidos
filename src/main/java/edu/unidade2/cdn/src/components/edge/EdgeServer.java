package edu.unidade2.cdn.src.components.edge;

import edu.unidade2.cdn.src.config.Color;
import edu.unidade2.cdn.src.model.EdgeResponse;
import edu.unidade2.security.handler.HandshakeHandler;
import edu.unidade2.security.SecureMessageFacade;
import edu.unidade2.security.keys.KeyGeneratorUtil;
import edu.unidade2.security.model.SessionKeys;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EdgeServer {
    // Configurações da CDN
    private final String geoDnsIp;
    private final int geoDnsPort;
    private final String originIp;
    private final int originPort;

    // Configurações locais deste Edge
    private final String myIp;
    private final int clientPort;
    private final int controlPort;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long TTL_MILLIS = 20000;

    private KeyPair rsaKeyPair;

    public EdgeServer(String geoDnsIp, int geoDnsPort, String originIp, int originPort, String myIp, int clientPort, int controlPort) {
        this.geoDnsIp = geoDnsIp;
        this.geoDnsPort = geoDnsPort;
        this.originIp = originIp;
        this.originPort = originPort;
        this.myIp = myIp;
        this.clientPort = clientPort;
        this.controlPort = controlPort;

        start();
    }

    public void start(){
        System.out.println(Color.infoMessage("[EdgeServer] Iniciando Edge Server em " + myIp + " (Cliente: " + clientPort + ", Controle: " + controlPort + ")"));

        // 0 - Gera o par de chaves RSA para comunicação segura
        try {
            System.out.println(Color.infoMessage("[EdgeServer] Gerando chaves RSA..."));
            this.rsaKeyPair = KeyGeneratorUtil.gerarParRSA();
        } catch (Exception e) {
            System.out.println(Color.securityMessage("Erro fatal ao gerar chaves RSA: " + e.getMessage()));
            System.exit(1);
        }

        // 1. Registra o Shutdown Hook para saída graciosa
        shutdownGracefully();

        // 2. Registra o Edge no GeoDNS ao ligar
        notifyGeoDns("REGISTER " + myIp + ":" + clientPort);

        // 3. Inicia as Threads de escuta
        new Thread(this::listenForClients).start();
        new Thread(this::listenForOriginControl).start();
    }

    private void shutdownGracefully() {
        System.out.println(Color.infoMessage("[EdgeServer] Registrando Shutdown Hook para saída graciosa..."));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println(Color.infoMessage("[EdgeServer] Encerrando Edge Server..."));
            notifyGeoDns("UNREGISTER " + myIp + ":" + clientPort);
        }));
    }


    private void notifyGeoDns(String command) {
        try (Socket socket = new Socket(geoDnsIp, geoDnsPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            SessionKeys keys = HandshakeHandler.realizarComoCliente(in, out);

            String pacoteComando = SecureMessageFacade.empacotarMensagem(command, keys.aes(), keys.hmac());
            out.println(pacoteComando);

            System.out.println(Color.successMessage("[EdgeServer] Comando seguro enviado para GeoDNS: " + command));

        } catch (SecurityException se) {
            System.out.println(Color.securityMessage("[EdgeServer] Falha de segurança ao notificar GeoDNS: " + se.getMessage()));
        } catch (Exception e) {
            System.out.println(Color.errorMessage("[EdgeServer] Erro ao enviar comando para GeoDNS: " + e.getMessage()));
        }
    }

    private void listenForClients() {
        try (ServerSocket serverSocket = new ServerSocket(clientPort)) {
            System.out.println(Color.infoMessage("[EdgeServer] Servidor de clientes iniciado na porta " + clientPort));
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> processClientRequest(clientSocket)).start();
            }
        } catch (Exception e) {
            System.out.println(Color.errorMessage("[EdgeServer] Erro ao iniciar servidor de clientes: " + e.getMessage()));
        }
    }

    private void processClientRequest(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            SessionKeys keys = HandshakeHandler.realizarComoServidor(in, out, rsaKeyPair);

            String pacoteRequisicao = in.readLine();
            if (pacoteRequisicao == null) return;

            String requisicao = SecureMessageFacade.desempacotarMensagem(pacoteRequisicao, keys.aes(), keys.hmac());

            if (!requisicao.startsWith("GET ") || !requisicao.split(" ")[1].matches("^/arquivo\\d+\\.ext$")) {
                System.out.println(Color.warningMessage("[EdgeServer] Requisição inválida recebida: " + requisicao));
                EdgeResponse response = new EdgeResponse("400 Bad Request", "MISS", 0, "");
                out.println(SecureMessageFacade.empacotarMensagem(response.toJson(), keys.aes(), keys.hmac()));
                return;
            }

            String caminhoArquivo = requisicao.split(" ")[1];
            CacheEntry entry = cache.get(caminhoArquivo);

            if (entry != null && (System.currentTimeMillis() - entry.timestamp() < TTL_MILLIS)) {
                System.out.println("[Edge] Cache HIT Seguro para " + caminhoArquivo);
                EdgeResponse response = new EdgeResponse("200 OK", "HIT", entry.data().length(), entry.data());
                out.println(SecureMessageFacade.empacotarMensagem(response.toJson(), keys.aes(), keys.hmac()));
            } else {
                System.out.println("[Edge] Cache MISS Seguro para " + caminhoArquivo);

                // Busca na origem usando canal seguro
                String dataFromOrigin = fetchFromOriginSecure(caminhoArquivo);

                if (dataFromOrigin != null && !dataFromOrigin.isEmpty()) {
                    cache.put(caminhoArquivo, new CacheEntry(dataFromOrigin, System.currentTimeMillis()));
                    EdgeResponse response = new EdgeResponse("200 OK", "MISS", dataFromOrigin.length(), dataFromOrigin);
                    out.println(SecureMessageFacade.empacotarMensagem(response.toJson(), keys.aes(), keys.hmac()));
                } else {
                    EdgeResponse response = new EdgeResponse("404 Not Found", "MISS", 0, "");
                    out.println(SecureMessageFacade.empacotarMensagem(response.toJson(), keys.aes(), keys.hmac()));
                }
            }
        } catch (Exception e) {
            System.out.println(Color.errorMessage("Erro ao processar requisição do cliente: " + e.getMessage()));
        }
    }

    private String fetchFromOriginSecure(String caminhoArquivo) {
        try (Socket socket = new Socket(originIp, originPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            SessionKeys keys = HandshakeHandler.realizarComoCliente(in, out);

            String requisicao = "GET " + caminhoArquivo;
            out.println(SecureMessageFacade.empacotarMensagem(requisicao, keys.aes(), keys.hmac()));

            String pacoteResposta = in.readLine();
            if (pacoteResposta != null) {
                return SecureMessageFacade.desempacotarMensagem(pacoteResposta, keys.aes(), keys.hmac());
            }
        } catch (SecurityException se) {
            System.out.println(Color.securityMessage("[EdgeServer] Falha de integridade ao buscar na Origem."));
        } catch (Exception e) {
            System.out.println(Color.errorMessage("[EdgeServer] Erro ao buscar da origem: " + e.getMessage()));
        }
        return "";
    }

    private void listenForOriginControl() {
        try (ServerSocket serverSocket = new ServerSocket(controlPort)) {
            System.out.println(Color.infoMessage(" [EdgeServer] Escutando comandos da Origem TCP na porta " + controlPort));
            while (true) {
                Socket originSocket = serverSocket.accept();
                new Thread(() -> processControlCommand(originSocket)).start();
            }
        } catch (Exception e) {
            System.out.println(Color.errorMessage("[EdgeServer] Erro ao iniciar servidor de controle da Origem: " + e.getMessage()));
        }
    }

    private void processControlCommand(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {

            SessionKeys keys = HandshakeHandler.realizarComoServidor(in, out, rsaKeyPair);

            String pacoteComando = in.readLine();
            if (pacoteComando != null) {
                String comando = SecureMessageFacade.desempacotarMensagem(pacoteComando, keys.aes(), keys.hmac());
                if (comando.startsWith("INVALIDATE ")) {
                    String caminhoArquivo = comando.split(" ")[1];
                    cache.remove(caminhoArquivo);
                    System.out.println(Color.warningMessage("[EdgeServer] Cache INVALIDADO com segurança para: " + caminhoArquivo));
                }
            }
        } catch (SecurityException se) {
            System.out.println(Color.securityMessage("[EdgeServer] Comando de controle rejeitado: " + se.getMessage()));
        } catch (Exception e) {
            System.out.println(Color.errorMessage("Erro ao processar comando de controle: " + e.getMessage()));
        }
    }
}

record CacheEntry(String data, long timestamp) {}
