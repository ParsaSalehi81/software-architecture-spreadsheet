package spreadsheet;

public final class Cell {
    private final Coordinate coordinate;
    private Content content;

    public Cell(Coordinate coordinate) {
        if (coordinate == null) {
            throw new IllegalArgumentException("A cell needs a coordinate.");
        }
        this.coordinate = coordinate;
        content = new TextContent("");
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public String getColumn() {
        return coordinate.getColumn();
    }

    public int getRow() {
        return coordinate.getRow();
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        if (content == null) {
            throw new IllegalArgumentException("Cell content cannot be null.");
        }
        this.content = content;
    }

    public void clear() {
        content = new TextContent("");
    }
}
