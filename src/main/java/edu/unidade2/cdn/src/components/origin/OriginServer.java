package edu.unidade2.cdn.src.components.origin;

import edu.unidade2.cdn.src.config.Color;
import edu.unidade2.cdn.src.model.Livro;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class OriginServer {
    private final int port;
    private final Map<String, Livro> database = new ConcurrentHashMap<>();

    private final String edgeIp;
    private final int edgeControlPort;

    public OriginServer(int port, String edgeIp, int edgeControlPort) {
        this.edgeIp = edgeIp;
        this.edgeControlPort = edgeControlPort;
        this.port = port;
        start("src/main/java/edu/unidade2/cdn/src/components/origin/resources/livros.csv");
    }

    public void start(String caminhoArquivo) {
        carregarBancodeDados(caminhoArquivo);
        iniciarTerminalAdmin();
        iniciarServidorTCP();
    }

    private void carregarBancodeDados(String caminhoArquivo) {
        System.out.println(Color.infoMessage("[OriginServer] Carregando banco de dados..."));
        try (BufferedReader br = new BufferedReader(new FileReader(caminhoArquivo))) {
            String linha;
            boolean primeiraLinha = true;

            while ((linha = br.readLine()) != null) {
                if (primeiraLinha) { // Pula o cabeçalho do CSV
                    primeiraLinha = false;
                    continue;
                }

                String[] dados = linha.split(",");
                if (dados.length == 5) {
                    String id = dados[0].trim();
                    Livro livro = new Livro(
                            id,
                            dados[1].trim(),
                            dados[2].trim(),
                            dados[3].trim(),
                            dados[4].trim()
                    );
                    database.put("/" + id, livro);
                }
            }
            System.out.println(Color.successMessage("[OriginServer] Banco de dados carregado com sucesso!"));
        } catch (Exception e) {
            System.out.println(Color.errorMessage("[OriginServer] Erro ao carregar banco de dados: " + e.getMessage()));
        }
    }

    private void iniciarTerminalAdmin() {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            System.out.println(Color.warningMessage("\n[Terminal] Digite 'INVALIDATE /arquivoX.ext' para forçar a limpeza de cache na borda."));

            while (scanner.hasNextLine()) {
                String comando = scanner.nextLine();
                if (comando != null && comando.startsWith("INVALIDATE ")) {
                    String caminhoArquivo = comando.split(" ")[1];
                    dispararInvalidacao(comando, caminhoArquivo);
                }
            }
        }).start();
    }

    private void dispararInvalidacao(String comando, String caminhoArquivo) {
        try (Socket socket = new Socket(edgeIp, edgeControlPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(comando);
            System.out.println(Color.successMessage("[OriginServer] Comando de invalidação enviado para a borda: " + caminhoArquivo));
        } catch (Exception e) {
            System.out.println(Color.errorMessage("[OriginServer] Erro ao enviar comando de invalidação: " + e.getMessage()));
        }
    }

    private void iniciarServidorTCP() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println(Color.infoMessage("[OriginServer] Servidor de Origem escutando TCP na porta " + port));

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> processarRequisicao(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processarRequisicao(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String requisicao = in.readLine();

            if (requisicao != null && requisicao.startsWith("GET ")) {
                String caminhoArquivo = requisicao.split(" ")[1];

                Livro livro = database.get(caminhoArquivo);

                if (livro != null) {
                    out.println(livro.toJson());
                    System.out.println(Color.successMessage("[OriginServer] Requisição atendida para " + caminhoArquivo));
                } else {
                    out.println("");
                    System.out.println(Color.warningMessage("[OriginServer] Arquivo não encontrado: " + caminhoArquivo));
                }
            }
        }catch (Exception e) {
            System.out.println(Color.errorMessage("Erro ao processar cliente: " + e.getMessage()));
        }
    }
}
