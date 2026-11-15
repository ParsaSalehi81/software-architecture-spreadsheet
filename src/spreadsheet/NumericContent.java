package spreadsheet;

import java.math.BigDecimal;

public final class NumericContent extends Content {
    private final BigDecimal number;

    public NumericContent(String source) {
        super(source);
        if (!isNumber(source)) {
            throw new IllegalArgumentException("Enter an integer or real number, for example 12 or 1.2.");
        }
        number = new BigDecimal(source);
    }

    public static boolean isNumber(String source) {
        if (source == null || source.isEmpty()) {
            return false;
        }
        try {
            new BigDecimal(source);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public BigDecimal getNumber() {
        return number;
    }

    @Override
    public String getType() {
        return "number";
    }
}
