package io.github.milczekt1.corral.rules.jakarta.nojavaxpersistence.fixtures;

import java.math.BigDecimal;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/** MUST FLAG: a converter on the old interface is never registered by a jakarta provider. */
@Converter
public class JavaxMoneyConverter implements AttributeConverter<BigDecimal, String> {

    @Override
    public String convertToDatabaseColumn(BigDecimal attribute) {
        return attribute == null ? null : attribute.toPlainString();
    }

    @Override
    public BigDecimal convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new BigDecimal(dbData);
    }
}
