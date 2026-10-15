package edu.unidade2.security.integrity;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class HmacUtil {

    private static final String ALGORITMO_HMAC = "HmacSHA256";

    public static String gerarAssinatura(String dadosCifradosBase64, SecretKey chaveHMAC) throws Exception {
        Mac mac = Mac.getInstance(ALGORITMO_HMAC);
        mac.init(chaveHMAC);
        byte[] hash = mac.doFinal(dadosCifradosBase64.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    public static boolean validarAssinatura(String dadosCifradosBase64, String hmacRecebido, SecretKey chaveHMAC) throws Exception {
        String hmacCalculado = gerarAssinatura(dadosCifradosBase64, chaveHMAC);
        // MessageDigest.isEqual previne ataques de timing (Timing Attacks)
        return MessageDigest.isEqual(
                hmacCalculado.getBytes(StandardCharsets.UTF_8),
                hmacRecebido.getBytes(StandardCharsets.UTF_8)
        );
    }
}
