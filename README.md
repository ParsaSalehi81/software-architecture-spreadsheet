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
content. Empty text clears a cell. Save before New, Load or Exit; there is no
automatic save. Saving to an existing filename replaces that file. A failed
load keeps the current sheet.

Try loading `examples/project-example.s2v`, viewing `A1`, changing `C1` to the
number `10`, saving to `copy.s2v`, then loading that copy and viewing `C1`.
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

The brief leaves some text cases ambiguous. This program quotes text that looks
like a number, starts with `=`, or contains `;`, quotes or line breaks. Quoted
fields always load as text; quotes inside them are doubled. Inside quoted text,
`\\`, `\n` and `\r` represent backslash and line breaks. Formulas use those
escapes too, plus `\,` for a literal comma. For example, text `12` saves as
`"12"`, while a number saves as `12`. This extension preserves contents without
changing ordinary files from the brief. It should be checked with the professor
if a different quoting convention is required.

## Check

```sh
javac -encoding UTF-8 -d out src/spreadsheet/*.java test/spreadsheet/*.java
java -cp out spreadsheet.SpreadsheetTests
```

Or use `sh test.sh` on Linux/macOS. Tests cover domain objects, the brief's
example, cell positions, all content types, save/load round trips, malformed
files, file errors and console operations.
