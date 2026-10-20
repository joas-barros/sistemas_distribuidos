package edu.unidade2.security;

import edu.unidade2.security.crypto.SymmetricCipher;
import edu.unidade2.security.integrity.HmacUtil;

import javax.crypto.SecretKey;

public class SecureMessageFacade {

    public static String empacotarMensagem(String mensagemPlana, SecretKey chaveAES, SecretKey chaveHMAC) throws Exception {
        String payloadCifrado = SymmetricCipher.cifrarTexto(mensagemPlana, chaveAES);
        String assinatura = HmacUtil.gerarAssinatura(payloadCifrado, chaveHMAC);
        return payloadCifrado + ":" + assinatura;
    }

    public static String desempacotarMensagem(String pacoteRecebido, SecretKey chaveAES, SecretKey chaveHMAC) throws Exception {
        String[] partes = pacoteRecebido.split(":");
        if (partes.length != 2) {
            throw new IllegalArgumentException("Formato de pacote inválido.");
        }

        String payloadCifrado = partes[0];
        String hmacRecebido = partes[1];

        if (!HmacUtil.validarAssinatura(payloadCifrado, hmacRecebido, chaveHMAC)) {
            throw new SecurityException("Falha de integridade: HMAC inválido!");
        }

        return SymmetricCipher.decifrarTexto(payloadCifrado, chaveAES);
    }
}
