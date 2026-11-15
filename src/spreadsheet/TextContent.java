package spreadsheet;

public final class TextContent extends Content {
    public TextContent(String text) {
        super(text);
    }

    public String getText() {
        return getSource();
    }

    @Override
    public String getType() {
        return "text";
    }
}
