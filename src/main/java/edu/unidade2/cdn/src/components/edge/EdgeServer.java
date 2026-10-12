package edu.unidade2.cdn.src.components.edge;

import edu.unidade2.cdn.src.config.Color;
import edu.unidade2.cdn.src.model.EdgeResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
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
    private static final long TTL_MILLIS = 10000;

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

        // 1. Registra o Shutdown Hook para saída graciosa
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println(Color.infoMessage("[EdgeServer] Encerrando Edge Server..."));
            notifyGeoDns("UNREGISTER " + myIp + ":" + clientPort);
        }));

        // 2. Registra o Edge no GeoDNS ao ligar
        notifyGeoDns("REGISTER " + myIp + ":" + clientPort);

        // 3. Inicia as Threads de escuta
        new Thread(this::listenForClients).start();
        new Thread(this::listenForOriginControl).start();
    }

    private void notifyGeoDns(String command) {
        try (Socket socket = new Socket(geoDnsIp, geoDnsPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(command);
            System.out.println(Color.successMessage("[EdgeServer] Comando enviado para GeoDNS: " + command));
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
            String requisicao = in.readLine();

            if(requisicao != null && requisicao.startsWith("GET ")) {
                String caminhoArquivo = requisicao.split(" ")[1];
                CacheEntry entry = cache.get(caminhoArquivo);

                if (entry != null && (System.currentTimeMillis() - entry.timestamp() < TTL_MILLIS)) {
                    System.out.println(Color.infoMessage("[EdgeServer] Cache HIT para " + caminhoArquivo));

                    EdgeResponse response = new EdgeResponse("200 OK", "HIT", entry.data().length(), entry.data());
                    out.println(response.toJson());
                } else {
                    System.out.println(Color.infoMessage("[EdgeServer] Cache MISS para " + caminhoArquivo));

                    String dataFromOrigin = fetchFromOrigin(caminhoArquivo);

                    if (!dataFromOrigin.isEmpty()) {
                        cache.put(caminhoArquivo, new CacheEntry(dataFromOrigin, System.currentTimeMillis()));

                        EdgeResponse response = new EdgeResponse("200 OK", "MISS", dataFromOrigin.length(), dataFromOrigin);
                        out.println(response.toJson());
                        System.out.println(Color.successMessage("[EdgeServer] Arquivo " + caminhoArquivo + " armazenado no cache."));
                    } else {
                        EdgeResponse response = new EdgeResponse("404 Not Found", "MISS", 0, "");
                        out.println(response.toJson());
                        System.out.println(Color.errorMessage("[EdgeServer] Arquivo " + caminhoArquivo + " não encontrado na Origem."));
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(Color.errorMessage("Erro ao processar requisição do cliente: " + e.getMessage()));
        }
    }

    private String fetchFromOrigin(String caminhoArquivo) {
        try (Socket originSocket = new Socket(originIp, originPort);
             PrintWriter out = new PrintWriter(originSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(originSocket.getInputStream()))) {

            out.println("GET " + caminhoArquivo);
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                responseBuilder.append(line).append("\n");
            }
            return responseBuilder.toString().trim();
        } catch (Exception e) {
            System.out.println(Color.errorMessage("[EdgeServer] Erro ao buscar arquivo da Origem: " + e.getMessage()));
            return "";
        }
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
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String command = in.readLine();
            if (command != null && command.startsWith("INVALIDATE ")) {
                String caminhoArquivo = command.split(" ")[1];
                cache.remove(caminhoArquivo);
                System.out.println(Color.successMessage("[EdgeServer] Cache de " + caminhoArquivo + " invalidado por comando da Origem."));
            }
        } catch (Exception e) {
            System.out.println(Color.errorMessage("Erro ao processar comando de controle: " + e.getMessage()));
        }
    }
}

record CacheEntry(String data, long timestamp) {}
