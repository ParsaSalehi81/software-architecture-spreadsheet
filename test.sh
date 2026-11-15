#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out src/spreadsheet/*.java test/spreadsheet/*.java
java -cp out spreadsheet.SpreadsheetTests
