package learntime.backend.global.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EncryptionUtilTest {

    @BeforeEach
    void setUp() {
        EncryptionUtil util = new EncryptionUtil();
        // 32바이트 키 설정 (AES-256)
        util.setKey("12345678901234567890123456789012");
    }

    @Test
    void testEncryptDecrypt() {
        String originalText = "Hello World";
        String encrypted = EncryptionUtil.encrypt(originalText);
        assertNotNull(encrypted);
        assertNotEquals(originalText, encrypted);

        String decrypted = EncryptionUtil.decrypt(encrypted);
        assertEquals(originalText, decrypted);
    }

    @Test
    void testEncryptDecryptWithNull() {
        assertNull(EncryptionUtil.encrypt(null));
        assertNull(EncryptionUtil.decrypt(null));
    }
}
