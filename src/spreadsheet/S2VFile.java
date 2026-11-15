package spreadsheet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Reads and writes cell contents. It never evaluates a formula. */
public final class S2VFile {
    private S2VFile() {
    }

    public static Spreadsheet load(Path path) throws IOException {
        Spreadsheet sheet = new Spreadsheet();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int row = 1;
            while ((line = reader.readLine()) != null) {
                if (row == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1); // Accept a UTF-8 byte order mark.
                }
                try {
                    List<Content> contents = parseRow(line);
                    for (int column = 1; column <= contents.size(); column++) {
                        sheet.getCell(Coordinate.at(column, row)).setContent(contents.get(column - 1));
                    }
                } catch (IllegalArgumentException | IOException e) {
                    throw new IOException("Invalid S2V at row " + row + ": " + e.getMessage(), e);
                }
                row++;
            }
        }
        return sheet;
    }

    public static void save(Spreadsheet sheet, Path path) throws IOException {
        Path target = path.toAbsolutePath();
        Path parent = target.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            throw new IOException("The destination folder does not exist.");
        }
        if (Files.isDirectory(target)) {
            throw new IOException("The destination must be a file, not a folder.");
        }

        // Finish writing before replacing an existing file.
        Path temporary = Files.createTempFile(parent, ".spreadsheet-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                int lastRow = sheet.getLastUsedRow();
                for (int row = 1; row <= lastRow; row++) {
                    int lastColumn = sheet.getLastUsedColumn(row);
                    for (int column = 1; column <= lastColumn; column++) {
                        if (column > 1) {
                            writer.write(';');
                        }
                        Content content = sheet.getCell(Coordinate.at(column, row)).getContent();
                        writer.write(encode(content));
                    }
                    writer.newLine();
                }
            }
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String encode(Content content) {
        String source = content.getSource();
        if (content instanceof FormulaContent) {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < source.length(); i++) {
                char ch = source.charAt(i);
                switch (ch) {
                    case ';': result.append(','); break;
                    case ',': result.append("\\,"); break;
                    case '\\': result.append("\\\\"); break;
                    case '\n': result.append("\\n"); break;
                    case '\r': result.append("\\r"); break;
                    default: result.append(ch);
                }
            }
            return result.toString();
        }

        if (!(content instanceof TextContent) || source.isEmpty()) {
            return source;
        }
        boolean needsQuotes = source.startsWith("=") || NumericContent.isNumber(source)
                || source.indexOf(';') >= 0 || source.indexOf('"') >= 0
                || source.indexOf('\n') >= 0 || source.indexOf('\r') >= 0;
        if (!needsQuotes) {
            return source;
        }

        StringBuilder result = new StringBuilder("\"");
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            switch (ch) {
                case '"': result.append("\"\""); break;
                case '\\': result.append("\\\\"); break;
                case '\n': result.append("\\n"); break;
                case '\r': result.append("\\r"); break;
                default: result.append(ch);
            }
        }
        return result.append('"').toString();
    }

    private static List<Content> parseRow(String line) throws IOException {
        List<Content> result = new ArrayList<>();
        int position = 0;
        while (true) {
            Content content;
            if (position < line.length() && line.charAt(position) == '"') {
                StringBuilder text = new StringBuilder();
                position++;
                boolean closed = false;
                while (position < line.length()) {
                    char ch = line.charAt(position++);
                    if (ch == '"') {
                        if (position < line.length() && line.charAt(position) == '"') {
                            text.append('"');
                            position++;
                        } else {
                            closed = true;
                            break;
                        }
                    } else if (ch == '\\') {
                        if (position == line.length()) {
                            throw new IOException("Incomplete escape in quoted text.");
                        }
                        text.append(decodeEscape(line.charAt(position++), false));
                    } else {
                        text.append(ch);
                    }
                }
                if (!closed) {
                    throw new IOException("Quoted text is missing its closing quote.");
                }
                if (position < line.length() && line.charAt(position) != ';') {
                    throw new IOException("Expected a semicolon after quoted text.");
                }
                content = new TextContent(text.toString());
            } else {
                int start = position;
                while (position < line.length() && line.charAt(position) != ';') {
                    position++;
                }
                String field = line.substring(start, position);
                content = field.startsWith("=")
                        ? new FormulaContent(decodeFormula(field)) : Content.fromSource(field);
            }
            result.add(content);
            if (position == line.length()) {
                return result;
            }
            position++; // Skip the cell separator; a trailing empty field is kept.
        }
    }

    private static String decodeFormula(String field) throws IOException {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < field.length(); i++) {
            char ch = field.charAt(i);
            if (ch == '\\') {
                if (++i == field.length()) {
                    throw new IOException("Incomplete escape in formula content.");
                }
                result.append(decodeEscape(field.charAt(i), true));
            } else {
                result.append(ch == ',' ? ';' : ch);
            }
        }
        return result.toString();
    }

    private static char decodeEscape(char escaped, boolean formula) throws IOException {
        switch (escaped) {
            case 'n': return '\n';
            case 'r': return '\r';
            case '\\': return '\\';
            case ',':
                if (formula) {
                    return ',';
                }
                break;
            default:
                break;
        }
        throw new IOException("Unknown escape: \\" + escaped);
    }
}
