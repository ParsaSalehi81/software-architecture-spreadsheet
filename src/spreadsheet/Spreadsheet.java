package spreadsheet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class Spreadsheet {
    // Only accessed cells need objects. Other valid coordinates are empty cells.
    private final Map<Coordinate, Cell> cells = new TreeMap<>();

    public Cell getCell(Coordinate coordinate) {
        if (coordinate == null) {
            throw new IllegalArgumentException("A coordinate is required.");
        }
        Cell cell = findCell(coordinate);
        if (cell == null) {
            cell = new Cell(coordinate);
            cells.put(coordinate, cell);
        }
        return cell;
    }

    /** Returns an existing cell, or null, without creating an empty cell. */
    public Cell findCell(Coordinate coordinate) {
        if (coordinate == null) {
            throw new IllegalArgumentException("A coordinate is required.");
        }
        return cells.get(coordinate);
    }

    public Cell getCell(String coordinate) {
        return getCell(Coordinate.parse(coordinate));
    }

    public void setContent(String coordinate, Content content) {
        getCell(coordinate).setContent(content);
    }

    public void clearCell(String coordinate) {
        getCell(coordinate).clear();
    }

    public List<Cell> getNonEmptyCells() {
        List<Cell> result = new ArrayList<>();
        for (Cell cell : cells.values()) {
            if (!cell.getContent().isEmpty()) {
                result.add(cell);
            }
        }
        return result;
    }

    public int getLastUsedRow() {
        int last = 0;
        for (Cell cell : cells.values()) {
            if (!cell.getContent().isEmpty() && cell.getRow() > last) {
                last = cell.getRow();
            }
        }
        return last;
    }

    public int getLastUsedColumn(int row) {
        int last = 0;
        for (Cell cell : cells.values()) {
            if (cell.getRow() == row && !cell.getContent().isEmpty()) {
                last = Math.max(last, cell.getCoordinate().getColumnIndex());
            }
        }
        return last;
    }
}
