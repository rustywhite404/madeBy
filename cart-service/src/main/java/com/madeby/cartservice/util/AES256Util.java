package com.madeby.cartservice.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class AES256Util {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY = "1234567890123456"; // 16자리 키

    // 랜덤 IV 생성 메서드
    public static IvParameterSpec generateRandomIV() {
        byte[] iv = new byte[16]; // 16바이트
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    // 암호화 (IV 포함)
    public static String encryptWithIV(String data) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);

        // 랜덤 IV 생성
        IvParameterSpec ivSpec = generateRandomIV();
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        String encryptedData = Base64.getEncoder().encodeToString(encryptedBytes);
        String iv = Base64.getEncoder().encodeToString(ivSpec.getIV());

        // IV + 암호화된 데이터 결합
        return iv + ":" + encryptedData;
    }

    // 복호화 (IV 포함된 데이터 처리)
    public static String decryptWithIV(String encryptedDataWithIV) throws Exception {
        String[] parts = encryptedDataWithIV.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid encrypted data format");
        }

        String iv = parts[0];
        String encryptedData = parts[1];

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(), ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(Base64.getDecoder().decode(iv));
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes);
    }

    // SHA-256 해시 생성
    public static String hashEmail(String email) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(email.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hash);
    }
}
