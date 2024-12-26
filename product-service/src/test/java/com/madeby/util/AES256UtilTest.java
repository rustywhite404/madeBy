package com.madeby.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AES256UtilTest {

    @Test
    void testEncryptionAndDecryptionWithIV() {
        // 테스트 데이터
        String originalText = "HelloWorld123!";

        try {
            // 암호화
            String encryptedDataWithIV = AES256Util.encryptWithIV(originalText);
            assertNotNull(encryptedDataWithIV, "암호화된 데이터가 null이어서는 안됩니다.");
            assertNotEquals(originalText, encryptedDataWithIV, "암호화된 데이터가 원본 텍스트와 같아서는 안됩니다.");

            // 복호화
            String decryptedText = AES256Util.decryptWithIV(encryptedDataWithIV);
            assertNotNull(decryptedText, "복호화된 텍스트가 null이어서는 안됩니다.");
            assertEquals(originalText, decryptedText, "복호화된 텍스트는 원본 텍스트와 같아야 합니다.");
        } catch (Exception e) {
            fail("예외가 발생했습니다: " + e.getMessage());
        }
    }

    @Test
    void testEmptyStringEncryptionWithIV() {
        String originalText = "";

        try {
            // 암호화
            String encryptedDataWithIV = AES256Util.encryptWithIV(originalText);
            assertNotNull(encryptedDataWithIV, "암호화된 빈 문자열이 null이어서는 안됩니다.");

            // 복호화
            String decryptedText = AES256Util.decryptWithIV(encryptedDataWithIV);
            assertEquals(originalText, decryptedText, "복호화된 빈 문자열은 원본 텍스트와 같아야 합니다.");
        } catch (Exception e) {
            fail("예외가 발생했습니다: " + e.getMessage());
        }
    }

    @Test
    void testNullEncryptionWithIV() {
        assertThrows(NullPointerException.class, () -> {
            AES256Util.encryptWithIV(null);
        }, "null 값 암호화 시 NullPointerException이 발생해야 합니다.");
    }

    @Test
    void testIVIncludedInEncryptedData() {
        // 테스트 데이터
        String originalText = "TestWithIV";

        try {
            // 암호화
            String encryptedDataWithIV = AES256Util.encryptWithIV(originalText);

            // IV와 암호화된 데이터가 결합된 포맷인지 확인
            String[] parts = encryptedDataWithIV.split(":");
            assertEquals(2, parts.length, "암호화된 데이터에는 IV와 데이터가 포함되어야 합니다.");
            assertNotNull(parts[0], "IV 부분이 null이어서는 안됩니다.");
            assertNotNull(parts[1], "암호화된 데이터 부분이 null이어서는 안됩니다.");

            // 복호화
            String decryptedText = AES256Util.decryptWithIV(encryptedDataWithIV);
            assertEquals(originalText, decryptedText, "복호화된 텍스트는 원본 텍스트와 같아야 합니다.");
        } catch (Exception e) {
            fail("예외가 발생했습니다: " + e.getMessage());
        }
    }
}
