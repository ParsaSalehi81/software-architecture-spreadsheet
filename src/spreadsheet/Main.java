package spreadsheet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/** A small console interface for the Step 2 operations. */
public final class Main {
    private final Scanner input = new Scanner(System.in);
    private Spreadsheet sheet = new Spreadsheet();
    private Path filePath;
    private boolean changed;

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        System.out.println("Spreadsheet project - Step 2");
        System.out.println("Mohammad Parsa Salehi");
        boolean running = true;
        while (running) {
            printMenu();
            String option = ask("Option: ");
            if (option == null) {
                if (changed) {
                    System.out.println("Input closed. Unsaved changes were not saved.");
                }
                break;
            }
            try {
                switch (option.trim()) {
                    case "1":
                        if (canLeaveSheet()) {
                            sheet = new Spreadsheet();
                            filePath = null;
                            changed = false;
                            System.out.println("New blank spreadsheet.");
                        }
                        break;
                    case "2": setContent(); break;
                    case "3": viewCell(); break;
                    case "4": viewAllContents(); break;
                    case "5": clearCell(); break;
                    case "6": loadSpreadsheet(); break;
                    case "7": saveSpreadsheet(); break;
                    case "0": running = !canLeaveSheet(); break;
                    default: System.out.println("Choose one of the menu options.");
                }
            } catch (IOException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        input.close();
        System.out.println("Goodbye.");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1. New spreadsheet");
        System.out.println("2. Set cell content");
        System.out.println("3. View cell content");
        System.out.println("4. View all non-empty cell contents");
        System.out.println("5. Clear cell");
        System.out.println("6. Load S2V file");
        System.out.println("7. Save S2V file");
        System.out.println("0. Exit");
    }

    private String ask(String prompt) {
        System.out.print(prompt);
        return input.hasNextLine() ? input.nextLine() : null;
    }

    private void setContent() {
        String address = ask("Coordinate (for example A1 or AA3): ");
        if (address == null) {
            return;
        }
        Coordinate coordinate = Coordinate.parse(address);
        String type = ask("Type: 1 = number, 2 = text, 3 = formula: ");
        if (type == null) {
            return;
        }
        type = type.trim();
        if (!type.equals("1") && !type.equals("2") && !type.equals("3")) {
            throw new IllegalArgumentException("Choose content type 1, 2 or 3.");
        }
        String source = ask("Content: ");
        if (source == null) {
            return;
        }
        Content content;
        if (type.equals("1")) {
            content = new NumericContent(source);
        } else if (type.equals("2")) {
            content = new TextContent(source);
        } else {
            content = new FormulaContent(source);
        }
        sheet.getCell(coordinate).setContent(content);
        changed = true;
        printCell(sheet.getCell(coordinate));
    }

    private void viewCell() {
        String address = ask("Coordinate: ");
        if (address != null) {
            printCell(sheet.getCell(address));
        }
    }

    private void viewAllContents() {
        List<Cell> cells = sheet.getNonEmptyCells();
        if (cells.isEmpty()) {
            System.out.println("The spreadsheet is empty.");
        }
        for (Cell cell : cells) {
            printCell(cell);
        }
    }

    private void printCell(Cell cell) {
        Content content = cell.getContent();
        System.out.println(cell.getCoordinate() + " [" + content.getType() + "]: "
                + content.getSource());
    }

    private void clearCell() {
        String address = ask("Coordinate to clear: ");
        if (address == null) {
            return;
        }
        Cell cell = sheet.getCell(address);
        if (cell.getContent().isEmpty()) {
            System.out.println("The cell is already empty.");
        } else {
            cell.clear();
            changed = true;
            System.out.println("Cell cleared.");
        }
    }

    private void loadSpreadsheet() throws IOException {
        String name = ask("S2V file to load (empty to cancel): ");
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        Path path = Paths.get(name.trim());
        if (!canLeaveSheet()) {
            return;
        }
        // Do not replace the active sheet until the entire file has loaded.
        Spreadsheet loaded = S2VFile.load(path);
        sheet = loaded;
        filePath = path;
        changed = false;
        System.out.println("Loaded: " + path);
    }

    private boolean saveSpreadsheet() throws IOException {
        String prompt = filePath == null
                ? "Save to file (empty to cancel): "
                : "Save to file (Enter uses " + filePath + "): ";
        String name = ask(prompt);
        if (name == null || (name.trim().isEmpty() && filePath == null)) {
            return false;
        }
        Path path = name.trim().isEmpty() ? filePath : Paths.get(name.trim());
        if (Files.exists(path)) {
            String overwrite = ask("File exists. Overwrite? (y/n): ");
            if (!"y".equalsIgnoreCase(overwrite)) {
                System.out.println("Save cancelled.");
                return false;
            }
        }
        S2VFile.save(sheet, path);
        filePath = path;
        changed = false;
        System.out.println("Saved: " + path);
        return true;
    }

    private boolean canLeaveSheet() throws IOException {
        if (!changed) {
            return true;
        }
        while (true) {
            String choice = ask("Unsaved changes: (s)ave, (d)iscard or (c)ancel? ");
            if (choice == null || "c".equalsIgnoreCase(choice)) {
                return false;
            }
            if ("d".equalsIgnoreCase(choice)) {
                return true;
            }
            if ("s".equalsIgnoreCase(choice)) {
                return saveSpreadsheet();
            }
            System.out.println("Choose s, d or c.");
        }
    }
}
