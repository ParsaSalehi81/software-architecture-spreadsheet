package spreadsheet;

/** The original cell content. This is not a calculated cell value. */
public abstract class Content {
    private final String source;

    protected Content(String source) {
        if (source == null) {
            throw new IllegalArgumentException("Cell content cannot be null.");
        }
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public boolean isEmpty() {
        return source.isEmpty();
    }

    public abstract String getType();

    /** Type detection used for ordinary S2V fields. */
    public static Content fromSource(String source) {
        if (source == null) {
            throw new IllegalArgumentException("Cell content cannot be null.");
        }
        if (source.startsWith("=")) {
            return new FormulaContent(source);
        }
        if (NumericContent.isNumber(source)) {
            return new NumericContent(source);
        }
        return new TextContent(source);
    }
}
