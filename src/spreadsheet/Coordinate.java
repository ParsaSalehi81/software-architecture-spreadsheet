package spreadsheet;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A spreadsheet address, such as A1 or AA12. Rows and columns start at 1. */
public final class Coordinate implements Comparable<Coordinate> {
    private static final Pattern ADDRESS = Pattern.compile("([A-Za-z]+)([1-9][0-9]*)");

    private final String column;
    private final int columnIndex;
    private final int row;

    public Coordinate(String column, int row) {
        if (column == null || !column.matches("[A-Za-z]+") || row < 1) {
            throw new IllegalArgumentException("Use column letters and a row starting at 1.");
        }
        this.column = column.toUpperCase(Locale.ROOT);
        this.row = row;

        long index = 0;
        for (int i = 0; i < this.column.length(); i++) {
            index = index * 26 + this.column.charAt(i) - 'A' + 1;
            if (index > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Column is too large.");
            }
        }
        this.columnIndex = (int) index;
    }

    public static Coordinate parse(String address) {
        if (address == null) {
            throw new IllegalArgumentException("A cell coordinate is required.");
        }
        Matcher match = ADDRESS.matcher(address.trim());
        if (!match.matches()) {
            throw new IllegalArgumentException("Invalid coordinate. Examples: A1, B12, AA3.");
        }
        try {
            return new Coordinate(match.group(1), Integer.parseInt(match.group(2)));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Row is too large.", e);
        }
    }

    public static Coordinate at(int columnIndex, int row) {
        if (columnIndex < 1 || row < 1) {
            throw new IllegalArgumentException("Rows and columns start at 1.");
        }
        StringBuilder letters = new StringBuilder();
        int index = columnIndex;
        while (index > 0) {
            index--;
            letters.append((char) ('A' + index % 26));
            index /= 26;
        }
        return new Coordinate(letters.reverse().toString(), row);
    }

    public String getColumn() {
        return column;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public int getRow() {
        return row;
    }

    @Override
    public int compareTo(Coordinate other) {
        int rowOrder = Integer.compare(row, other.row);
        return rowOrder == 0 ? Integer.compare(columnIndex, other.columnIndex) : rowOrder;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Coordinate)) {
            return false;
        }
        Coordinate coordinate = (Coordinate) other;
        return row == coordinate.row && columnIndex == coordinate.columnIndex;
    }

    @Override
    public int hashCode() {
        return 31 * row + columnIndex;
    }

    @Override
    public String toString() {
        return column + row;
    }
}
