package spreadsheet;

public final class CellRange {
    private final Coordinate topLeft;
    private final Coordinate bottomRight;

    public CellRange(Coordinate topLeft, Coordinate bottomRight) {
        if (topLeft == null || bottomRight == null) {
            throw new IllegalArgumentException("A range needs two coordinates.");
        }
        if (topLeft.getRow() > bottomRight.getRow()
                || topLeft.getColumnIndex() > bottomRight.getColumnIndex()) {
            throw new IllegalArgumentException("Give the top-left corner before the bottom-right corner.");
        }
        this.topLeft = topLeft;
        this.bottomRight = bottomRight;
    }

    public static CellRange parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("A cell or range is required.");
        }
        String[] corners = text.split(":", -1);
        if (corners.length == 1) {
            Coordinate cell = Coordinate.parse(corners[0]);
            return new CellRange(cell, cell);
        }
        if (corners.length == 2) {
            return new CellRange(Coordinate.parse(corners[0]), Coordinate.parse(corners[1]));
        }
        throw new IllegalArgumentException("Invalid range. Example: A1:B3.");
    }

    public Coordinate getTopLeft() {
        return topLeft;
    }

    public Coordinate getBottomRight() {
        return bottomRight;
    }

    public boolean contains(Coordinate coordinate) {
        return coordinate.getRow() >= topLeft.getRow()
                && coordinate.getRow() <= bottomRight.getRow()
                && coordinate.getColumnIndex() >= topLeft.getColumnIndex()
                && coordinate.getColumnIndex() <= bottomRight.getColumnIndex();
    }

    @Override
    public String toString() {
        return topLeft + ":" + bottomRight;
    }
}
