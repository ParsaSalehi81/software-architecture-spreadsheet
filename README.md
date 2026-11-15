# Spreadsheet project - Step 2

**Mohammad Parsa Salehi**

This deliverable stores and edits spreadsheet **contents**. For example, a cell
containing =1+2 is displayed as =1+2, not as 3.

It includes the Java classes from the Step 1 domain model, a small console menu,
and S2V loading and saving. It uses Java's standard library, with no Maven,
Gradle or external dependencies.

## Run

You need a JDK, version 11 or later. Open a terminal in this project folder:

    javac -encoding UTF-8 -d out src/spreadsheet/*.java
    java -cp out spreadsheet.Main

On Linux or macOS, you can also run:

    sh run.sh

The program starts with a blank spreadsheet. Its menu supports:

1. Create a new spreadsheet.
2. Set a cell's content, choosing number, text or formula.
3. View one cell's original content.
4. View all non-empty cell contents, ordered by row and then column.
5. Clear a cell.
6. Load an S2V file.
7. Save an S2V file.
0. Exit.

Coordinates such as A1, Z12 and AA3 work. New, Load and Exit ask about unsaved
changes. A failed load keeps the current sheet. Saving finishes writing a
temporary file before replacing the destination.

## Try the required operations

1. Run the program and choose 6 (Load).
2. Enter examples/project-example.s2v.
3. Choose 3 (View cell), then A1. It shows:

       A1 [formula]: =C1+C2

4. View C3. It shows =A1+B1, not a calculated result.
5. Choose 2 (Set), enter C1, choose type 1 (number), and enter 10.
6. Choose 7 (Save) and enter a new path, such as example-copy.s2v.
7. Create a new sheet, load that saved file, and view C1 again. Its content is 10.

To enter numeric-looking text such as 12, choose the **text** content type.
The content type and the original characters are preserved when saving and
loading, including numeric formatting such as 001.2300.

## Classes

| Class | Purpose |
| --- | --- |
| Spreadsheet | Keeps the sheet's cells and finds its used rows and columns. |
| Cell | Has one coordinate and one current Content. |
| Content | Abstract parent that keeps the original content string. |
| TextContent | Stores text, including the empty string used by empty cells. |
| NumericContent | Stores a number and preserves its original text. |
| FormulaContent | Stores the original expression beginning with =. |
| FunctionCall | Represents a function name, raw arguments, ranges and nested calls. |
| CellRange | Represents an inclusive rectangle, or a single-cell range. |

Coordinate is a small helper for the column and row data used by Cell and
CellRange. S2VFile handles local files, and Main handles the console menu.

Function names are restricted to SUMA, MIN, MAX and PROMEDIO. A nested call must
have the same FormulaContent owner as its parent and cannot have two parents
or form a nesting cycle.

For this step, formula expressions are stored as strings. FunctionCall and
CellRange provide the domain structures for later parsing; the editor does
not automatically build a formula tree. Formula syntax checks, dependency
analysis, circular-reference checks, results and recalculation belong to the
later calculation work. No formula is evaluated, even during loading.

## S2V format

The ordinary format follows the project brief:

- A physical file line represents one spreadsheet row.
- Semicolons separate cells, including leading and internal empty fields.
- Each row ends at its last non-empty cell.
- Blank rows before later data keep their positions.
- An empty file represents a blank sheet.
- Formula expressions are saved, not results.
- Inside formula fields, function-argument semicolons are saved as commas
  and restored when loading. Commas in ordinary text are not changed.
- Files use UTF-8; the reader also accepts a UTF-8 byte order mark.

The example from the brief is saved unchanged:

    =C1+C2;4;1
    ;;2
    ;TOTAL;=A1+B1

### Convention for the ambiguity identified in Step 1

The brief does not define quoting or how to keep text such as 12 distinct
from numeric content. This implementation uses a small, documented extension:

- Text is quoted when it contains a semicolon, quotation mark or line break,
  starts with =, or would otherwise be read as a number.
- A quoted field always becomes TextContent.
- A quotation mark inside quoted text is doubled.
- Inside quoted text, backslash, line feed and carriage return are written as
  `\\`, `\n` and `\r` respectively, without adding physical spreadsheet rows.
- Formula fields also escape backslashes and line breaks. A literal comma in
  an unparsed formula is escaped as `\,`, so it does not get confused with an
  argument separator from the file.

For example, text 12 is saved as "12"; numeric content 12 is saved as 12.
Text left;right is saved as "left;right". These conventions let those contents
round-trip rather than rejecting valid text or silently changing its type.
Ordinary files from the brief do not need these extra rules. If the professor
specifies another escaping convention, the change is isolated in S2VFile.

The console accepts one line of input at a time. Line breaks inside content
are supported through the model and the file format.

## Tests

From this project folder:

    javac -encoding UTF-8 -d out src/spreadsheet/*.java test/spreadsheet/*.java
    java -cp out spreadsheet.SpreadsheetTests

Or, on Linux/macOS:

    sh test.sh

There are nine test scenarios: coordinates/ranges, domain relationships,
the brief's sample, function separators and empty rows, mixed-content round
trips, imported files, malformed files, failed/empty saves, and console
operations. The round-trip tests cover numbers, formulas, numeric-looking
text, semicolons, quotes, Unicode, backslashes and embedded line breaks.
