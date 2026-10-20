package edu.unidade2.cdn.src.components.client;

import edu.unidade2.cdn.src.config.Color;
import edu.unidade2.security.SecureMessageFacade;
import edu.unidade2.security.handler.HandshakeHandler;
import edu.unidade2.security.handler.UdpSecurityHandler;
import edu.unidade2.security.model.SessionKeys;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.security.PublicKey;
import java.util.Scanner;

public class Cliente {
    private final String geoDnsIp;
    private final int geoDnsPort;

    private String cachedEdgeAddress = null;

    public Cliente(String geoDnsIp, int geoDnsPort) {
        this.geoDnsIp = geoDnsIp;
        this.geoDnsPort = geoDnsPort;

        iniciarInterface();
    }

    public void iniciarInterface() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=== Cliente CDN Iniciado ===");

        while (true) {
            System.out.print("\nDigite o caminho do arquivo que deseja buscar (ex.: /arquivo10.ext) ou 'sair' para encerrar: ");
            String entrada = scanner.nextLine().trim();

            if (entrada.equalsIgnoreCase("sair")) {
                System.out.println("Encerrando o cliente...");
                break;
            }

            // Valida se a entrada segue o padrão exato: começa com /arquivo, tem números e termina com .ext
            if (!entrada.matches("^/arquivo\\d+\\.ext$")) {
                System.out.println("Erro na entrada de dados. Tente outra vez!");
                continue;
            }

            requisitarArquivo(entrada);
        }
        scanner.close();
    }

    public void requisitarArquivo(String filename) {
        // Tenta executar a requisição com um máximo de 2 tentativas em caso de falha do Edge
        int maxTentativas = 2;

        for (int tentativa = 1; tentativa <= maxTentativas; tentativa++) {
            if (cachedEdgeAddress == null) {
                resolverEdgeNoGeoDNS(filename);
            }

            if (cachedEdgeAddress == null) {
                System.out.println(Color.errorMessage("Não foi possível obter um Edge Server do GeoDNS."));
                return;
            }

            String[] partes = cachedEdgeAddress.split(":");
            String edgeIp = partes[0];
            int edgePort = Integer.parseInt(partes[1]);

            try (
                    Socket socket = new Socket(edgeIp, edgePort);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
            ) {
                System.out.println(Color.infoMessage("[Cliente] Conectado ao Edge. Iniciando Handshake RSA..."));

                // 1. HANDSHAKE SEGURO
                SessionKeys keys = HandshakeHandler.realizarComoCliente(in, out);

                // 2. ENVIO CIFRADO
                String comando = "GET " + filename;
                String pacoteRequisicao = SecureMessageFacade.empacotarMensagem(comando, keys.aes(), keys.hmac());

//                pacoteRequisicao = pacoteRequisicao.substring(0, pacoteRequisicao.length() - 1) + "X";
                out.println(pacoteRequisicao);

                System.out.println(Color.infoMessage("[Cliente] Aguardando resposta segura do Edge " + cachedEdgeAddress + "..."));

                // 3. RECEPÇÃO E DECIFRAGEM
                String pacoteResposta = in.readLine();

                if (pacoteResposta != null) {
                    String respostaJson = SecureMessageFacade.desempacotarMensagem(pacoteResposta, keys.aes(), keys.hmac());

                    if (respostaJson.equals("Erro na entrada de dados. Tente outra vez!")) {
                        System.out.println(respostaJson);
                    } else {
                        exibirTabelaFormatada(respostaJson);
                    }
                }
                break;
            } catch (SecurityException se) {
                System.out.println(Color.securityMessage("Integridade comprometida ao falar com o Edge: " + se.getMessage()));
                cachedEdgeAddress = null;
            } catch (Exception e) {
                System.out.println(Color.errorMessage("[Falha] Erro ao ligar ao Edge " + cachedEdgeAddress + ": " + e.getMessage()));
                cachedEdgeAddress = null;

                if (tentativa == maxTentativas) {
                    System.err.println(Color.errorMessage("[Cliente] Todas as tentativas falharam."));
                } else {
                    System.out.println(Color.warningMessage("A tentar novamente com um novo Edge Server..."));
                }
            }
        }
    }

    private void exibirTabelaFormatada(String json) {
        String status = extrairValorJSON(json, "\"status\": \"", "\"");
        String cache = extrairValorJSON(json, "\"cache\": \"", "\"");

        System.out.println("\n+--------------------------------------------------+");
        System.out.printf("| STATUS: %-15s | CACHE: %-13s |\n", status, cache);
        System.out.println("+--------------------------------------------------+");

        if (status.contains("200")) {
            String nome = extrairValorJSON(json, "\"nome\": \"", "\"");
            String autor = extrairValorJSON(json, "\"autor\": \"", "\"");
            String ano = extrairValorJSON(json, "\"lancamento\": ", ",");
            String desc = extrairValorJSON(json, "\"descricao\": \"", "\"");

            System.out.printf("| %-8s : %-37s |\n", "Título", nome);
            System.out.printf("| %-8s : %-37s |\n", "Autor", autor);
            System.out.printf("| %-8s : %-37s |\n", "Ano", ano);

            // Quebra a descrição se for muito longa para manter o layout limpo
            if (desc.length() > 37) {
                desc = desc.substring(0, 34) + "...";
            }
            System.out.printf("| %-8s : %-37s |\n", "Resumo", desc);
            System.out.println("+--------------------------------------------------+");
        } else {
            System.out.printf("| %-48s |\n", "Arquivo não encontrado na base de dados.");
            System.out.println("+--------------------------------------------------+");
        }
    }

    private String extrairValorJSON(String json, String chaveInicio, String delimitadorFim) {
        int indiceInicio = json.indexOf(chaveInicio);
        if (indiceInicio == -1) return "N/A";

        indiceInicio += chaveInicio.length();
        int indiceFim = json.indexOf(delimitadorFim, indiceInicio);

        if (indiceFim == -1) return "N/A";
        return json.substring(indiceInicio, indiceFim).trim();
    }

    private void resolverEdgeNoGeoDNS(String filename) {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            InetAddress geoDnsAddress = InetAddress.getByName(geoDnsIp);
            udpSocket.setSoTimeout(2000);

            System.out.println(Color.infoMessage("[Cliente] Solicitando chave pública do GeoDNS..."));

            // 1. OBTENÇÃO DA CHAVE PÚBLICA
            PublicKey pubKey = UdpSecurityHandler.solicitarChavePublica(udpSocket, geoDnsAddress, geoDnsPort);

            System.out.println(Color.infoMessage("[Cliente] Consultando GeoDNS de forma segura..."));

            // 2. CONSULTA PROTEGIDA
            String mensagem = "WHERE " + filename;
            String resposta = UdpSecurityHandler.enviarEReceberSeguro(udpSocket, geoDnsAddress, geoDnsPort, mensagem, pubKey);

            if (resposta.startsWith("ERROR") || resposta.equals("Erro na entrada de dados. Tente outra vez!")) {
                System.out.println(Color.errorMessage("[GeoDNS] " + resposta));
            } else {
                System.out.println(Color.successMessage("[DNS Cache] Novo Edge Server resolvido: " + resposta));
                this.cachedEdgeAddress = resposta;
            }
        } catch (SecurityException se) {
            System.out.println(Color.securityMessage("Falha de segurança na consulta UDP: " + se.getMessage()));
        } catch (Exception e) {
            System.out.println(Color.errorMessage("Erro ao consultar o GeoDNS via UDP: " + e.getMessage()));
        }
    }
}
