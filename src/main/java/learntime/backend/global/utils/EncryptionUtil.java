package learntime.backend.global.utils;

import learntime.backend.global.error.code.ErrorCode;
import learntime.backend.global.error.exception.EncryptionException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EncryptionUtil {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    // 프로젝트 설정 파일(yml)에서 관리하는 것을 권장하며, 현재는 테스트용 키(32자)를 사용합니다.
    private static final String KEY = "12345678901234567890123456789012";
    private static final String IV = KEY.substring(0, 16);

    public static String encrypt(String text) {
        try {
            if (text == null) return null;
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), "AES");
            IvParameterSpec ivParamSpec = new IvParameterSpec(IV.getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParamSpec);
            byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new EncryptionException(ErrorCode.ENCRYPTION_ERROR, "암호화 중 발생: " + e.getMessage());
        }
    }

    public static String decrypt(String cipherText) {
        try {
            if (cipherText == null) return null;
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), "AES");
            IvParameterSpec ivParamSpec = new IvParameterSpec(IV.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParamSpec);
            byte[] decodedBytes = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decodedBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException(ErrorCode.ENCRYPTION_ERROR, "복호화 중 발생: " + e.getMessage());
        }
    }
}
