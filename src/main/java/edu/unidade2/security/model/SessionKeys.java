package edu.unidade2.security.model;

import javax.crypto.SecretKey;

public record SessionKeys(
        SecretKey aes,
        SecretKey hmac
) {
}
