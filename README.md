# Spreadsheet project — Step 2

**Mohammad Parsa Salehi**

A small Java console program to set and view cell **contents**, and load/save
spreadsheets. For example, `=1+2` stays `=1+2`: no formulas are evaluated.
Only Java's standard library is used.

## Run

Install a JDK (Java 11 or later), then run from this folder:

```sh
javac -encoding UTF-8 -d out src/spreadsheet/*.java
java -cp out spreadsheet.Main
```

On Linux/macOS, `sh run.sh` does the same thing.

The menu offers New, Set, View, List, Clear, Load, Save and Exit. Set asks for
a coordinate (such as `A1` or `AA3`), a type (number, text or formula), and its
content. Empty text clears a cell. New, Load and Exit offer Save, Discard or
Cancel if there are unsaved changes. Failed/cancelled saves stop the pending
action. Save can reuse the last path and asks before overwriting. A failed load
keeps the current sheet. Empty load paths cancel; `-` cancels Save.

Try loading `examples/project-example.s2v`, viewing `A1`, changing `C1` to the
number `10`, saving to `copy.s2v`, then loading that copy and viewing `C1`.
Press Enter at each format prompt to use the brief's standard S2V.
`examples/functions.s2v` also includes all four functions and a nested function.

## Classes

The Step 1 domain classes are `Spreadsheet`, `Cell`, `Content`, `TextContent`,
`NumericContent`, `FormulaContent`, `FunctionCall` and `CellRange`.
`Coordinate` handles cell addresses, `S2VFile` handles files, and `Main` is the menu.

`FunctionCall` supports SUMA, MIN, MAX and PROMEDIO, arguments, ranges and nested
calls. A nested call shares its parent's formula owner and cannot have two
parents or form a cycle. These structures are ready for later formula parsing;
Step 2 stores expressions as strings without building or evaluating a formula
tree. Formula syntax, dependencies and calculated results are left for later.

## File format

S2V uses UTF-8, one line per row and `;` between cells. Empty positions and
intervening blank rows are kept; trailing empty cells are omitted. An empty
sheet saves as an empty file. Formula argument separators change from `;` to
`,` on save and back on load. Ordinary text commas stay unchanged.

Standard S2V is the default. Quotes and backslashes are ordinary characters,
not escape syntax. Types are inferred: a field starting with `=` becomes a
formula, a numeric representation becomes a number, and other fields become
text. The original characters of numbers are kept. A leading `U+FEFF` is kept
as cell content rather than automatically removed as a byte order mark.

The brief does not say how text `12` stays distinct from the number `12`, or
how to store text containing `;` or line breaks. Standard mode writes `12`
as-is, so it reloads as a number; text starting with `=` similarly reloads as
a formula. Standard mode reports an error for embedded text delimiters/line
breaks instead of writing a file with different cell positions. The previous
file and active contents stay intact. These format questions need clarification
from the professor; valid text can still be edited and saved in quoted mode.

At a Load or Save format prompt, **q** explicitly selects the custom quoted
extension. Use it only for files written with that convention, not for the
professor's standard files. It preserves text types and special characters:
text that looks numeric, starts with `=`, or contains `;`, quotes or line breaks
is quoted; doubled quotes represent a quote. Inside quoted text, `\\`, `\n`
and `\r` represent backslash and line breaks. Formulas use those escapes too,
plus `\,` for a literal comma. For example, text `12` saves as `"12"` in quoted
mode. The extension is not claimed to be part of the required S2V specification.

For programmatic use, `S2VFile.load(path)` and `save(sheet, path)` use standard
S2V. Pass `true` as the final argument to explicitly use the quoted extension.
Neither saving mode creates missing cell objects for empty positions.

## Check

```sh
javac -encoding UTF-8 -d out src/spreadsheet/*.java test/spreadsheet/*.java
java -cp out spreadsheet.SpreadsheetTests
```

Or use `sh test.sh` on Linux/macOS. Tests cover domain objects, the brief's
example, cell positions, both file modes, literal quotes/backslashes, leading
`U+FEFF`, saves without adding cells, file errors and console confirmations.
