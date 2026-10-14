package edu.unidade2.cdn.src.components.origin;

import edu.unidade2.cdn.src.config.Color;
import edu.unidade2.cdn.src.model.Livro;
import edu.unidade2.security.SecureMessageFacade;
import edu.unidade2.security.crypto.AsymmetricCipher;
import edu.unidade2.security.keys.KeyGeneratorUtil;

import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.util.Base64;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class OriginServer {
    private final int port;
    private final Map<String, Livro> database = new ConcurrentHashMap<>();

    private final String edgeIp;
    private final int edgeControlPort;

    private KeyPair rsaKeyPair;

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
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // Como a Origem atua como cliente neste caso, ela recebe a Pública do Edge
            String chavePublicaBase64 = in.readLine();

            // Gera chaves temporárias para a sessão
            SecretKey chaveAES = KeyGeneratorUtil.gerarChaveAES();
            SecretKey chaveHMAC = KeyGeneratorUtil.gerarChaveHMAC();

            // Cifra e envia as chaves
            String aesCifrada = AsymmetricCipher.cifrarComRSA(chaveAES.getEncoded(), chavePublicaBase64);
            String hmacCifrado = AsymmetricCipher.cifrarComRSA(chaveHMAC.getEncoded(), chavePublicaBase64);
            out.println(aesCifrada + ":" + hmacCifrado);

            // Envia o comando cifrado e assinado
            String pacoteComando = SecureMessageFacade.empacotarMensagem(comando, chaveAES, chaveHMAC);
            out.println(pacoteComando);

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
            // HANDSHAKE RSA

            // Envio da chave pública para o edge
            String chavePublicaStr = Base64.getEncoder().encodeToString(rsaKeyPair.getPublic().getEncoded());
            out.println(chavePublicaStr);

            // Aguarda o Edge enviar as chaves cifradas "AES:HMAC"
            String chavesCifradas = in.readLine();
            if (chavesCifradas == null) {;
                System.out.println(Color.securityMessage("[OriginServer] Falha no handshake: chaves não recebidas."));
                return;
            }

            String[] chavesPartes = chavesCifradas.split(":");
            if (chavesPartes.length != 2) {
                System.out.println(Color.securityMessage("[OriginServer] Falha no handshake: formato de chaves inválido."));
                return;
            }

            SecretKey chaveAES = AsymmetricCipher.decifrarChaveAESComRSA(chavesPartes[0], rsaKeyPair.getPrivate());
            SecretKey chaveHMAC = AsymmetricCipher.decifrarChaveHMACComRSA(chavesPartes[1], rsaKeyPair.getPrivate());

            // RECEPÇÃO E DESEMPACOTAMENTO SEGURO
            String pacoteRequisicao = in.readLine();
            if (pacoteRequisicao == null) {
                System.out.println(Color.securityMessage("[OriginServer] Falha na recepção: pacote de requisição não recebido."));
                return;
            }

            String requisicao = SecureMessageFacade.desempacotarMensagem(pacoteRequisicao, chaveAES, chaveHMAC);

            if (requisicao.startsWith("GET ")) {
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
        } catch (SecurityException se) {
            System.out.println(Color.securityMessage("[OriginServer] Erro de Segurança/HMAC Inválido. Conexão rejeitada."));
        } catch (Exception e) {
            System.out.println(Color.errorMessage("Erro ao processar cliente: " + e.getMessage()));
        }
    }
}
