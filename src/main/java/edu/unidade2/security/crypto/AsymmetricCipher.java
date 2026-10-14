package edu.unidade2.security.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class AsymmetricCipher {

    private static final String ALGORITMO_RSA = "RSA";
    private static final String ALGORITMO_AES = "AES";
    private static final String ALGORITMO_HMAC = "HmacSHA256";

    public static String cifrarComRSA(byte[] dados, String chavePublicaBase64) throws Exception {
        // Reconstrói a chave pública a partir da string Base64
        byte[] chaveBytes = Base64.getDecoder().decode(chavePublicaBase64);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(chaveBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey chavePublica = keyFactory.generatePublic(keySpec);

        // Cifra os dados
        Cipher cifra = Cipher.getInstance("RSA");
        cifra.init(Cipher.ENCRYPT_MODE, chavePublica);
        byte[] dadosCifrados = cifra.doFinal(dados);

        return Base64.getEncoder().encodeToString(dadosCifrados);
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

    public static java.security.PublicKey getPublicKeyFromBase64(String chavePublicaBase64) throws Exception {
        byte[] chaveBytes = Base64.getDecoder().decode(chavePublicaBase64);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(chaveBytes);
        return KeyFactory.getInstance(ALGORITMO_RSA).generatePublic(keySpec);
    }
}
