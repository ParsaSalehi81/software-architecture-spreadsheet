package spreadsheet;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Small tests using only Java's standard library. */
public final class SpreadsheetTests {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("spreadsheet-tests-");
        try {
            coordinatesAndRanges();
            domainObjects();
            projectExample(folder);
            functionsAndEmptyRows(folder);
            allContentsRoundTrip(folder);
            importedFiles(folder);
            malformedFiles(folder);
            emptyAndFailedSaves(folder);
            consoleOperations(folder);
            System.out.println("All tests passed (" + checks + " checks in 9 scenarios).");
        } finally {
            deleteTree(folder);
        }
    }

    private static void coordinatesAndRanges() throws Exception {
        equal("AA3", Coordinate.parse(" aa3 ").toString(), "normalise coordinate");
        equal(26, Coordinate.parse("Z1").getColumnIndex(), "column Z");
        equal(27, Coordinate.parse("AA1").getColumnIndex(), "column AA");
        equal("AZ2", Coordinate.at(52, 2).toString(), "column AZ");
        equal("BA2", Coordinate.at(53, 2).toString(), "column BA");
        check(Coordinate.parse("Z1").compareTo(Coordinate.parse("AA1")) < 0,
                "columns sort in spreadsheet order");
        expect(IllegalArgumentException.class, () -> Coordinate.parse("A0"), "zero row");
        expect(IllegalArgumentException.class, () -> Coordinate.parse("1A"), "reversed address");
        expect(IllegalArgumentException.class, () -> Coordinate.parse("A2147483648"), "row overflow");
        expect(IllegalArgumentException.class, () -> Coordinate.parse("ZZZZZZZZ1"), "column overflow");

        CellRange range = CellRange.parse("Z2:AB3");
        check(range.contains(Coordinate.parse("AA3")), "inclusive multi-letter range");
        check(!range.contains(Coordinate.parse("AA4")), "outside range");
        check(CellRange.parse("A1").contains(Coordinate.parse("A1")), "single-cell range");
        expect(IllegalArgumentException.class, () -> CellRange.parse("B3:A1"), "reversed corners");
    }

    private static void domainObjects() throws Exception {
        Spreadsheet sheet = new Spreadsheet();
        equal("", sheet.getCell("A1").getContent().getSource(), "empty cell content");
        equal(0, sheet.getLastUsedRow(), "viewing an empty cell does not create used data");
        sheet.setContent("AA2", new NumericContent("001.2300"));
        equal("001.2300", sheet.getCell("AA2").getContent().getSource(), "numeric source is kept");
        sheet.setContent("A1", new FormulaContent("=1+2"));
        equal("=1+2", sheet.getCell("A1").getContent().getSource(), "formula is not evaluated");
        sheet.clearCell("AA2");
        equal(1, sheet.getLastUsedRow(), "clearing the last used row updates bounds");
        equal("=1+2", sheet.getCell("A1").getContent().getSource(), "clear does not evaluate formulas");
        expect(IllegalArgumentException.class, () -> new NumericContent("TOTAL"), "invalid number");
        expect(IllegalArgumentException.class, () -> new FormulaContent("1+2"), "missing equals prefix");

        FormulaContent owner = new FormulaContent("=SUMA(A1:B2;MIN(C1:C2))");
        FunctionCall outer = owner.addFunctionCall("SUMA", "A1:B2");
        outer.addArgument("MIN(C1:C2)");
        outer.addRange(CellRange.parse("A1:B2"));
        FunctionCall nested = owner.addFunctionCall("MIN", "C1:C2");
        outer.addNestedCall(nested);
        equal(owner, nested.getFormula(), "nested call has the same formula owner");
        equal(outer, nested.getParent(), "nested call has one parent");
        equal(2, owner.getFunctionCalls().size(), "owner keeps both call occurrences");
        FormulaContent other = new FormulaContent("=MAX(A1)");
        FunctionCall foreign = other.addFunctionCall("MAX", "A1");
        expect(IllegalArgumentException.class, () -> outer.addNestedCall(foreign), "different formula owner");
        expect(IllegalArgumentException.class, () -> outer.addNestedCall(nested), "second parent");
        expect(IllegalArgumentException.class, () -> nested.addNestedCall(outer), "nested cycle");
        expect(IllegalArgumentException.class, () -> owner.addFunctionCall("AVERAGE", "A1"),
                "unsupported function name");
        expect(IllegalArgumentException.class, () -> owner.addFunctionCall("SUMA", ""),
                "function needs an argument");
    }

    private static void projectExample(Path folder) throws Exception {
        Path example = Paths.get("examples/project-example.s2v");
        Spreadsheet sheet = S2VFile.load(example);
        equal("=C1+C2", sheet.getCell("A1").getContent().getSource(), "A1 is its expression, not 3");
        equal("4", sheet.getCell("B1").getContent().getSource(), "B1 numeric content");
        equal("", sheet.getCell("A2").getContent().getSource(), "leading empty cell");
        equal("", sheet.getCell("B2").getContent().getSource(), "internal empty cell");
        equal("2", sheet.getCell("C2").getContent().getSource(), "row position");
        equal("TOTAL", sheet.getCell("B3").getContent().getSource(), "text content");
        equal("=A1+B1", sheet.getCell("C3").getContent().getSource(), "C3 is not evaluated to 7");
        Path saved = folder.resolve("project-copy.s2v");
        S2VFile.save(sheet, saved);
        equal(read(example), read(saved), "project example saves in the brief's exact layout");
    }

    private static void functionsAndEmptyRows(Path folder) throws Exception {
        Spreadsheet sheet = S2VFile.load(Paths.get("examples/functions.s2v"));
        equal("=SUMA(1;A1:B2;PROMEDIO(A1:B2))",
                sheet.getCell("A3").getContent().getSource(), "nested function separators restored");
        equal("=MIN(A1:B2)", sheet.getCell("B3").getContent().getSource(), "MIN content");
        equal("=MAX(A1:B2)", sheet.getCell("C3").getContent().getSource(), "MAX content");
        sheet.setContent("AA5", new TextContent("last"));
        Path saved = folder.resolve("functions-copy.s2v");
        S2VFile.save(sheet, saved);
        List<String> lines = Files.readAllLines(saved, StandardCharsets.UTF_8);
        equal(5, lines.size(), "one file line per spreadsheet row");
        equal("", lines.get(3), "intervening empty row is kept");
        equal(27, lines.get(4).split(";", -1).length, "multi-letter column position");
        Spreadsheet loaded = S2VFile.load(saved);
        equal("last", loaded.getCell("AA5").getContent().getSource(), "AA5 round trip");
        equal("", loaded.getCell("Z5").getContent().getSource(), "cell before AA remains empty");
    }

    private static void allContentsRoundTrip(Path folder) throws Exception {
        List<Content> contents = Arrays.asList(
                new TextContent("12"),
                new TextContent("-1.20"),
                new TextContent("=A1+2"),
                new TextContent("left;right"),
                new TextContent("say \"hello\""),
                new TextContent("hello,world"),
                new TextContent("first\nsecond\r\nthird"),
                new TextContent("C:\\new\\path;literal\\n"),
                new TextContent("  12  "),
                new TextContent("Mohammad Parsa Salehi"),
                new TextContent("سلام دنیا"),
                new TextContent("\""),
                new TextContent(";"),
                new TextContent("\\n"),
                new NumericContent("001.2300"),
                new NumericContent("-0"),
                new NumericContent("1.2E+3"),
                new FormulaContent("=SUMA(1;A1:B2;PROMEDIO(C1:C2))"),
                new FormulaContent("=1+2"),
                new FormulaContent("=B100+C9"),
                new FormulaContent("=SUMA(1,2)"),
                new FormulaContent("=raw\\content\nstill raw\r")
        );
        Spreadsheet original = new Spreadsheet();
        for (int i = 0; i < contents.size(); i++) {
            original.getCell(Coordinate.at(i + 1, 1)).setContent(contents.get(i));
        }
        Path file = folder.resolve("all-contents.s2v");
        S2VFile.save(original, file);
        equal(1, Files.readAllLines(file, StandardCharsets.UTF_8).size(),
                "embedded newlines do not create extra spreadsheet rows");
        Spreadsheet loaded = S2VFile.load(file);
        for (int i = 0; i < contents.size(); i++) {
            Content actual = loaded.getCell(Coordinate.at(i + 1, 1)).getContent();
            equal(contents.get(i).getType(), actual.getType(), "type survives at column " + (i + 1));
            equal(contents.get(i).getSource(), actual.getSource(), "content survives at column " + (i + 1));
        }
        Path copy = folder.resolve("all-contents-copy.s2v");
        S2VFile.save(loaded, copy);
        equal(read(file), read(copy), "repeated save/load is stable");
    }

    private static void importedFiles(Path folder) throws Exception {
        Path file = folder.resolve("import.s2v");
        Files.write(file, ("\uFEFF\"12\";12;hello,world;=SUMA(1,2);\r\n\r\n;;last\r\n")
                .getBytes(StandardCharsets.UTF_8));
        Spreadsheet sheet = S2VFile.load(file);
        equal("text", sheet.getCell("A1").getContent().getType(), "quoted numeric-looking text");
        equal("number", sheet.getCell("B1").getContent().getType(), "unquoted number");
        equal("hello,world", sheet.getCell("C1").getContent().getSource(), "text comma is not changed");
        equal("=SUMA(1;2)", sheet.getCell("D1").getContent().getSource(), "formula comma is restored");
        equal("", sheet.getCell("E1").getContent().getSource(), "trailing empty field");
        equal("", sheet.getCell("A2").getContent().getSource(), "empty physical row");
        equal("last", sheet.getCell("C3").getContent().getSource(), "CRLF and row positions");
    }

    private static void malformedFiles(Path folder) throws Exception {
        Path file = folder.resolve("broken.s2v");
        for (String invalid : Arrays.asList("\"not closed", "\"ok\"junk", "\"bad\\q\"", "=bad\\")) {
            Files.write(file, ("valid;row\n" + invalid + "\n").getBytes(StandardCharsets.UTF_8));
            expect(IOException.class, () -> S2VFile.load(file), "malformed S2V is reported");
        }
        expect(IOException.class, () -> S2VFile.load(folder.resolve("missing.s2v")), "missing file");
    }

    private static void emptyAndFailedSaves(Path folder) throws Exception {
        Path file = folder.resolve("empty.s2v");
        S2VFile.save(new Spreadsheet(), file);
        equal(0L, Files.size(file), "blank sheet saves as an empty file");
        equal(0, S2VFile.load(file).getNonEmptyCells().size(), "empty file loads as a blank sheet");
        Spreadsheet sheet = new Spreadsheet();
        sheet.setContent("A1", new TextContent("keep"));
        Path directory = folder.resolve("folder-not-file");
        Files.createDirectory(directory);
        Path marker = directory.resolve("keep.txt");
        Files.write(marker, "untouched".getBytes(StandardCharsets.UTF_8));
        expect(IOException.class, () -> S2VFile.save(sheet, directory), "folder is not a destination file");
        equal("untouched", read(marker), "failed save keeps existing data");
        expect(IOException.class, () -> S2VFile.save(sheet, folder.resolve("missing/f.s2v")),
                "missing destination folder");
        equal("keep", sheet.getCell("A1").getContent().getSource(), "failed save keeps active contents");
    }

    private static void consoleOperations(Path folder) throws Exception {
        Path saved = folder.resolve("console.s2v");
        String output = runConsole("6\nexamples/project-example.s2v\n3\nA1\n3\nC3\n"
                + "2\nC1\n1\n10\n3\nA1\n7\n" + saved + "\n0\n");
        check(output.contains("A1 [formula]: =C1+C2"), "console displays formula content");
        check(output.contains("C3 [formula]: =A1+B1"), "console does not replace content with a value");
        check(output.contains("Saved:"), "console save operation succeeds");
        equal("10", S2VFile.load(saved).getCell("C1").getContent().getSource(), "console edit is saved");

        output = runConsole("2\nA1\n2\nkeep me\n6\n" + folder.resolve("missing.s2v")
                + "\nd\n3\nA1\n0\nd\n");
        check(output.contains("Error:"), "console reports a failed load");
        check(output.contains("A1 [text]: keep me"), "failed load keeps the active sheet");

        output = runConsole("2\nA1\n2\nold content\n1\nc\n3\nA1\n0\nd\n");
        check(output.contains("A1 [text]: old content"), "cancelling New keeps unsaved contents");
    }

    private static String runConsole(String commands) throws Exception {
        String executable = Paths.get(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        Process process = new ProcessBuilder(executable, "-cp", System.getProperty("java.class.path"),
                "spreadsheet.Main").redirectErrorStream(true).start();
        try (OutputStream input = process.getOutputStream()) {
            input.write(commands.getBytes(StandardCharsets.UTF_8));
        }
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("Console test timed out.");
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        equal(0, process.exitValue(), "console exits normally");
        return output;
    }

    private static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void equal(Object expected, Object actual, String message) {
        check(expected.equals(actual), message + " (expected " + expected + ", got " + actual + ")");
    }

    private static void expect(Class<? extends Exception> type, Action action, String message)
            throws Exception {
        try {
            action.run();
        } catch (Exception e) {
            check(type.isInstance(e), message + " (wrong exception: " + e + ")");
            return;
        }
        throw new AssertionError(message + " (no exception)");
    }

    private static void deleteTree(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> children = Files.newDirectoryStream(path)) {
                for (Path child : children) {
                    deleteTree(child);
                }
            }
        }
        Files.delete(path);
    }

    private interface Action {
        void run() throws Exception;
    }
}
