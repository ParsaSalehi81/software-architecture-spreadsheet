package spreadsheet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** A console menu for editing contents and testing load/save. */
public final class Main {
    private final Scanner input = new Scanner(System.in);
    private Spreadsheet sheet = new Spreadsheet();
    private Path savedPath;
    private boolean changed;

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        System.out.println("Spreadsheet - Step 2 | Mohammad Parsa Salehi");
        while (true) {
            System.out.println("\n1. New  2. Set  3. View  4. List  5. Clear  6. Load  7. Save  0. Exit");
            try {
                switch (ask("Option: ").trim()) {
                    case "1":
                        if (canLeaveSheet()) {
                            sheet = new Spreadsheet();
                            savedPath = null;
                            changed = false;
                            System.out.println("New blank spreadsheet.");
                        }
                        break;
                    case "2": setContent(); break;
                    case "3": printCell(sheet.getCell(ask("Coordinate: "))); break;
                    case "4":
                        if (sheet.getNonEmptyCells().isEmpty()) {
                            System.out.println("The spreadsheet is empty.");
                        }
                        for (Cell cell : sheet.getNonEmptyCells()) {
                            printCell(cell);
                        }
                        break;
                    case "5":
                        sheet.clearCell(ask("Coordinate: "));
                        changed = true;
                        break;
                    case "6":
                        String name = ask("File to load (empty cancels): ");
                        if (!name.isEmpty()) {
                            boolean quoted = askQuotedMode();
                            if (canLeaveSheet()) {
                                // A failed load never replaces the active sheet.
                                sheet = S2VFile.load(Paths.get(name), quoted);
                                savedPath = Paths.get(name);
                                changed = false;
                                System.out.println("Loaded.");
                            }
                        }
                        break;
                    case "7": saveSpreadsheet(); break;
                    case "0":
                        if (canLeaveSheet()) {
                            input.close();
                            return;
                        }
                        break;
                    default: System.out.println("Choose a menu option.");
                }
            } catch (NoSuchElementException e) {
                if (changed) {
                    System.out.println("Input closed. Unsaved changes were not saved.");
                }
                input.close();
                return;
            } catch (IOException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private String ask(String prompt) {
        System.out.print(prompt);
        return input.nextLine();
    }

    private boolean askQuotedMode() {
        String mode = ask("Format: Enter = brief's S2V (types inferred), q = custom quoted format: ").trim();
        if (!mode.isEmpty() && !mode.equalsIgnoreCase("q")) {
            throw new IllegalArgumentException("Press Enter for standard S2V or enter q.");
        }
        return mode.equalsIgnoreCase("q");
    }

    private boolean saveSpreadsheet() throws IOException {
        String name = ask("Save path (Enter reuses " + savedPath + "; '-' cancels): ");
        if (name.equals("-") || (name.isEmpty() && savedPath == null)) {
            return false;
        }
        Path path = name.isEmpty() ? savedPath : Paths.get(name);
        if (Files.exists(path) && !ask("Overwrite existing file? (y/n): ").equalsIgnoreCase("y")) {
            return false;
        }
        S2VFile.save(sheet, path, askQuotedMode());
        savedPath = path;
        changed = false;
        System.out.println("Saved.");
        return true;
    }

    private boolean canLeaveSheet() throws IOException {
        if (!changed) {
            return true;
        }
        switch (ask("Unsaved changes: (s)ave, (d)iscard, (c)ancel: ").toLowerCase()) {
            case "s": return saveSpreadsheet();
            case "d": return true;
            default: return false;
        }
    }

    private void setContent() {
        Coordinate coordinate = Coordinate.parse(ask("Coordinate (for example A1 or AA3): "));
        String type = ask("Type: 1 = number, 2 = text, 3 = formula: ").trim();
        if (!type.equals("1") && !type.equals("2") && !type.equals("3")) {
            throw new IllegalArgumentException("Choose type 1, 2 or 3.");
        }
        String source = ask("Content (empty text clears the cell): ");
        Content content;
        switch (type) {
            case "1": content = new NumericContent(source); break;
            case "2": content = new TextContent(source); break;
            default: content = new FormulaContent(source);
        }
        sheet.getCell(coordinate).setContent(content);
        changed = true;
        printCell(sheet.getCell(coordinate));
    }

    private void printCell(Cell cell) {
        System.out.println(cell.getCoordinate() + " [" + cell.getContent().getType() + "]: "
                + cell.getContent().getSource());
    }
}
