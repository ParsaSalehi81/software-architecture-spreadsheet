package spreadsheet;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** A console menu for editing contents and testing load/save. */
public final class Main {
    private final Scanner input = new Scanner(System.in);
    private Spreadsheet sheet = new Spreadsheet();

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        System.out.println("Spreadsheet - Step 2 | Mohammad Parsa Salehi");
        System.out.println("Save before loading, starting a new sheet or exiting.");
        while (true) {
            System.out.println("\n1. New  2. Set  3. View  4. List  5. Clear  6. Load  7. Save  0. Exit");
            try {
                switch (ask("Option: ").trim()) {
                    case "1":
                        sheet = new Spreadsheet();
                        System.out.println("New blank spreadsheet.");
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
                    case "5": sheet.clearCell(ask("Coordinate: ")); break;
                    case "6":
                        // Assignment happens only after the complete file loads successfully.
                        sheet = S2VFile.load(Paths.get(ask("File to load: ")));
                        System.out.println("Loaded.");
                        break;
                    case "7":
                        S2VFile.save(sheet, Paths.get(ask("File to save (replaces existing file): ")));
                        System.out.println("Saved.");
                        break;
                    case "0": input.close(); return;
                    default: System.out.println("Choose a menu option.");
                }
            } catch (NoSuchElementException e) {
                input.close();
                return; // End of console input.
            } catch (IOException | IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private String ask(String prompt) {
        System.out.print(prompt);
        return input.nextLine();
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
        printCell(sheet.getCell(coordinate));
    }

    private void printCell(Cell cell) {
        System.out.println(cell.getCoordinate() + " [" + cell.getContent().getType() + "]: "
                + cell.getContent().getSource());
    }
}
