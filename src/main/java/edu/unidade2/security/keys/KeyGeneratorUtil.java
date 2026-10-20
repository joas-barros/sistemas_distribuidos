package edu.unidade2.security.keys;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public class KeyGeneratorUtil {

    private static final String ALGORITMO_RSA = "RSA";
    private static final String ALGORITMO_AES = "AES";
    private static final String ALGORITMO_HMAC = "HmacSHA256";

    public static KeyPair gerarParRSA() throws NoSuchAlgorithmException {
        KeyPairGenerator gerador = KeyPairGenerator.getInstance(ALGORITMO_RSA);
        gerador.initialize(2048);
        return gerador.generateKeyPair();
    }

    public static SecretKey gerarChaveAES() throws NoSuchAlgorithmException {
        KeyGenerator gerador = KeyGenerator.getInstance(ALGORITMO_AES);
        gerador.init(128);
        return gerador.generateKey();
    }

    public static SecretKey gerarChaveHMAC() throws NoSuchAlgorithmException {
        KeyGenerator gerador = KeyGenerator.getInstance(ALGORITMO_HMAC);
        return gerador.generateKey();
    }
}
