package edu.unidade2.security.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SymmetricCipher {

    private static final String ALGORITMO_AES = "AES";

    public static String cifrarTexto(String textoPlano, SecretKey chaveAES) throws Exception {
        Cipher cifra = Cipher.getInstance(ALGORITMO_AES);
        cifra.init(Cipher.ENCRYPT_MODE, chaveAES);
        byte[] textoCifrado = cifra.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(textoCifrado);
    }

    public static String decifrarTexto(String textoCifradoBase64, SecretKey chaveAES) throws Exception {
        Cipher cifra = Cipher.getInstance(ALGORITMO_AES);
        cifra.init(Cipher.DECRYPT_MODE, chaveAES);
        byte[] textoDecifrado = cifra.doFinal(Base64.getDecoder().decode(textoCifradoBase64));
        return new String(textoDecifrado, StandardCharsets.UTF_8);
    }
}
