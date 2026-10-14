package edu.unidade2.security.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class AsymmetricCipher {

    private static final String ALGORITMO_RSA = "RSA";
    private static final String ALGORITMO_AES = "AES";
    private static final String ALGORITMO_HMAC = "HmacSHA256";

    public static String cifrarChaveComRSA(SecretKey chaveSessao, PublicKey chavePublicaDestino) throws Exception {
        Cipher cifra = Cipher.getInstance(ALGORITMO_RSA);
        cifra.init(Cipher.ENCRYPT_MODE, chavePublicaDestino);
        byte[] chaveCifrada = cifra.doFinal(chaveSessao.getEncoded());
        return Base64.getEncoder().encodeToString(chaveCifrada);
    }

    public static SecretKey decifrarChaveAESComRSA(String chaveCifradaBase64, PrivateKey chavePrivada) throws Exception {
        Cipher cifra = Cipher.getInstance(ALGORITMO_RSA);
        cifra.init(Cipher.DECRYPT_MODE, chavePrivada);
        byte[] chaveDecifrada = cifra.doFinal(Base64.getDecoder().decode(chaveCifradaBase64));
        return new SecretKeySpec(chaveDecifrada, ALGORITMO_AES);
    }

    public static SecretKey decifrarChaveHMACComRSA(String chaveCifradaBase64, PrivateKey chavePrivada) throws Exception {
        Cipher cifra = Cipher.getInstance(ALGORITMO_RSA);
        cifra.init(Cipher.DECRYPT_MODE, chavePrivada);
        byte[] chaveDecifrada = cifra.doFinal(Base64.getDecoder().decode(chaveCifradaBase64));
        return new SecretKeySpec(chaveDecifrada, ALGORITMO_HMAC);
    }
}
