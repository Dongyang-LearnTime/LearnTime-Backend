package learntime.backend.global.common;

import learntime.backend.global.utils.EncryptionUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Objects;

@Converter
public class WeightConverter implements AttributeConverter<Double, String> {

    @Override
    public String convertToDatabaseColumn(Double attribute) {
        // DB에 저장할 때: Double 숫자를 암호화된 String으로 변환
        return (attribute == null) ? null : EncryptionUtil.encrypt(String.valueOf(attribute));
    }

    @Override
    public Double convertToEntityAttribute(String dbData) {
        // DB에서 읽어올 때: 암호화된 String을 복호화하여 다시 Double 숫자로 변환
        return (dbData == null) ? null : Double.parseDouble(Objects.requireNonNull(EncryptionUtil.decrypt(dbData), "복호화된 데이터가 null입니다."));
    }
}
