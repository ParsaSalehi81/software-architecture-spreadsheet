package spreadsheet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Domain structure for later formula parsing. It performs no calculation. */
public final class FunctionCall {
    private final String name;
    private final FormulaContent formula;
    private final List<String> arguments = new ArrayList<>();
    private final List<CellRange> ranges = new ArrayList<>();
    private final List<FunctionCall> nestedCalls = new ArrayList<>();
    private FunctionCall parent;

    // FormulaContent creates calls so every call has exactly one owning formula.
    FunctionCall(FormulaContent formula, String name, String firstArgument) {
        if (!"SUMA".equals(name) && !"MIN".equals(name)
                && !"MAX".equals(name) && !"PROMEDIO".equals(name)) {
            throw new IllegalArgumentException("Function must be SUMA, MIN, MAX or PROMEDIO.");
        }
        this.formula = formula;
        this.name = name;
        addArgument(firstArgument);
    }

    public String getName() {
        return name;
    }

    public FormulaContent getFormula() {
        return formula;
    }

    public FunctionCall getParent() {
        return parent;
    }

    public List<String> getArguments() {
        return Collections.unmodifiableList(arguments);
    }

    public void addArgument(String argument) {
        if (argument == null || argument.trim().isEmpty()) {
            throw new IllegalArgumentException("A function argument cannot be empty.");
        }
        arguments.add(argument);
    }

    public List<CellRange> getRanges() {
        return Collections.unmodifiableList(ranges);
    }

    public void addRange(CellRange range) {
        if (range == null) {
            throw new IllegalArgumentException("A range is required.");
        }
        ranges.add(range);
    }

    public List<FunctionCall> getNestedCalls() {
        return Collections.unmodifiableList(nestedCalls);
    }

    public void addNestedCall(FunctionCall child) {
        if (child == null || child.formula != formula) {
            throw new IllegalArgumentException("Nested calls must belong to the same formula.");
        }
        if (child.parent != null) {
            throw new IllegalArgumentException("This nested call already has a parent.");
        }
        for (FunctionCall current = this; current != null; current = current.parent) {
            if (current == child) {
                throw new IllegalArgumentException("Nested calls cannot contain a cycle.");
            }
        }
        child.parent = this;
        nestedCalls.add(child);
    }
}
