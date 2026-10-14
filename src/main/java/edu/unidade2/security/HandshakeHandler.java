package edu.unidade2.security;

import edu.unidade2.security.crypto.AsymmetricCipher;
import edu.unidade2.security.crypto.SymmetricCipher;
import edu.unidade2.security.keys.KeyGeneratorUtil;
import edu.unidade2.security.model.SessionKeys;

import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.security.KeyPair;
import java.util.Base64;

public class HandshakeHandler {

    public static SessionKeys realizarComoServidor(BufferedReader in, PrintWriter out, KeyPair rsaKeyPair) throws Exception {
        // 1. Envia a Chave Pública para o cliente
        String chavePublicaBase64 = Base64.getEncoder().encodeToString(rsaKeyPair.getPublic().getEncoded());

        // 2. Recebe as chaves AES e HMAC geradas pelo cliente e cifradas com a Pública
        String chavesCifradas = in.readLine();
        if (chavesCifradas == null) throw new SecurityException("Falha no Handshake: Conexão interrompida.");

        String[] partes = chavesCifradas.split(":");

        // 3. Decifra usando a Chave Privada
        SecretKey aes = AsymmetricCipher.decifrarChaveAESComRSA(partes[0], rsaKeyPair.getPrivate());
        SecretKey hmac = AsymmetricCipher.decifrarChaveHMACComRSA(partes[1], rsaKeyPair.getPrivate());

        return new SessionKeys(aes, hmac);
    }

    public static SessionKeys realizarComoCliente(BufferedReader in, PrintWriter out, KeyPair rsaKeyPair) throws Exception {
        // 1. Recebe a Chave Pública do servidor
        String chavePublicaBase64 = in.readLine();
        if (chavePublicaBase64 == null) throw new SecurityException("Falha no Handshake: Conexão interrompida.");

        // 2. Gera as chaves simétricas da sessão
        SecretKey aes = KeyGeneratorUtil.gerarChaveAES();
        SecretKey hmac = KeyGeneratorUtil.gerarChaveHMAC();

        // 3. Cifra as chaves com a Chave Pública do servidor
        String aesCifrada = AsymmetricCipher.cifrarComRSA(aes.getEncoded(), chavePublicaBase64);
        String hmacCifrada = AsymmetricCipher.cifrarComRSA(hmac.getEncoded(), chavePublicaBase64);

        // 4. Envia as chaves cifradas para o servidor
        out.println(aesCifrada + ":" + hmacCifrada);

        return new SessionKeys(aes, hmac);
    }
}
