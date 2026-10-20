package edu.unidade2.security.handler;

import edu.unidade2.security.SecureMessageFacade;
import edu.unidade2.security.crypto.AsymmetricCipher;
import edu.unidade2.security.keys.KeyGeneratorUtil;
import edu.unidade2.security.model.SessionKeys;

import javax.crypto.SecretKey;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;

public class UdpSecurityHandler {

    public static PublicKey solicitarChavePublica(DatagramSocket socket, InetAddress address, int port) throws Exception {
        byte[] reqBytes = "GET_PUB_KEY".getBytes();
        DatagramPacket reqPacket = new DatagramPacket(reqBytes, reqBytes.length, address, port);
        socket.send(reqPacket);

        byte[] buffer = new byte[2048];
        DatagramPacket resPacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(resPacket);

        String pubKeyBase64 = new String(resPacket.getData(), 0, resPacket.getLength()).trim();
        return AsymmetricCipher.getPublicKeyFromBase64(pubKeyBase64);
    }

    public static String enviarEReceberSeguro(DatagramSocket socket, InetAddress address, int port, String mensagem, PublicKey serverPubKey) throws Exception {
        // 1. Gera chaves da sessão
        SecretKey aesKey = KeyGeneratorUtil.gerarChaveAES();
        SecretKey hmacKey = KeyGeneratorUtil.gerarChaveHMAC();

        // 2. Cifra as chaves com a Pública do servidor
        String pubKeyBase64 = Base64.getEncoder().encodeToString(serverPubKey.getEncoded());
        String aesCifrada = AsymmetricCipher.cifrarComRSA(aesKey.getEncoded(), pubKeyBase64);
        String hmacCifrada = AsymmetricCipher.cifrarComRSA(hmacKey.getEncoded(), pubKeyBase64);

        // 3. Empacota a mensagem (AES + HMAC)
        String payload = SecureMessageFacade.empacotarMensagem(mensagem, aesKey, hmacKey);
        String pacoteFinal = aesCifrada + ":" + hmacCifrada + ":" + payload;

        // 4. Envia
        byte[] sendBytes = pacoteFinal.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendBytes, sendBytes.length, address, port);
        socket.send(sendPacket);

        // 5. Aguarda e desempacota a resposta
        byte[] buffer = new byte[4096];
        DatagramPacket resPacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(resPacket);

        String respostaCifrada = new String(resPacket.getData(), 0, resPacket.getLength()).trim();
        return SecureMessageFacade.desempacotarMensagem(respostaCifrada, aesKey, hmacKey);
    }

    public record UdpRequest(String mensagemPlana, SessionKeys keys) {}

    public static UdpRequest processarRequisicaoSegura(String requestBruta, KeyPair serverKeys) throws Exception {
        String[] partes = requestBruta.split(":");
        if (partes.length != 4) {
            throw new IllegalArgumentException("Formato de datagrama UDP inválido.");
        }

        String aesCifradaStr = partes[0];
        String hmacCifradaStr = partes[1];
        String mensagemEmpacotada = partes[2] + ":" + partes[3];

        SecretKey aesKey = AsymmetricCipher.decifrarChaveAESComRSA(aesCifradaStr, serverKeys.getPrivate());
        SecretKey hmacKey = AsymmetricCipher.decifrarChaveHMACComRSA(hmacCifradaStr, serverKeys.getPrivate());

        String mensagemPlana = SecureMessageFacade.desempacotarMensagem(mensagemEmpacotada, aesKey, hmacKey);

        return new UdpRequest(mensagemPlana, new SessionKeys(aesKey, hmacKey));
    }
}
