package com.example.galdcup.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

public class AES256Encryptor {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int KEY_SIZE = 32;
    private static final int IV_SIZE = 16;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    private AES256Encryptor(byte[] keyBytes) {
        if (keyBytes == null || keyBytes.length != KEY_SIZE) {
            throw new IllegalArgumentException("AES-256은 32바이트 키가 필요합니다.");
        }
        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /** Base64 인코딩된 키로 Encryptor 생성 */
    public static AES256Encryptor fromBase64Key(String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        return new AES256Encryptor(keyBytes);
    }

    /** 암호화 */
    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            byte[] ivPlusCipher = new byte[IV_SIZE + cipherBytes.length];
            System.arraycopy(iv, 0, ivPlusCipher, 0, IV_SIZE);
            System.arraycopy(cipherBytes, 0, ivPlusCipher, IV_SIZE, cipherBytes.length);

            return Base64.getEncoder().encodeToString(ivPlusCipher);
        } catch (Exception e) {
            throw new RuntimeException("AES256 암호화 실패", e);
        }
    }

    /** 복호화 */
    public String decrypt(String base64IvAndCipherText) {
        try {
            byte[] ivPlusCipher = Base64.getDecoder().decode(base64IvAndCipherText);

            byte[] iv = new byte[IV_SIZE];
            byte[] cipherBytes = new byte[ivPlusCipher.length - IV_SIZE];

            System.arraycopy(ivPlusCipher, 0, iv, 0, IV_SIZE);
            System.arraycopy(ivPlusCipher, IV_SIZE, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES 복호화 실패", e);
        }
    }
}