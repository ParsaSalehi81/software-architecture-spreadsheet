package spreadsheet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Stores an expression without parsing or evaluating it in Step 2. */
public final class FormulaContent extends Content {
    private final List<FunctionCall> functionCalls = new ArrayList<>();

    public FormulaContent(String expression) {
        super(expression);
        if (!expression.startsWith("=")) {
            throw new IllegalArgumentException("A formula must start with =.");
        }
    }

    public String getExpression() {
        return getSource();
    }

    public List<FunctionCall> getFunctionCalls() {
        return Collections.unmodifiableList(functionCalls);
    }

    // This structure can be populated when formula parsing is added later.
    public FunctionCall addFunctionCall(String name, String firstArgument) {
        FunctionCall call = new FunctionCall(this, name, firstArgument);
        functionCalls.add(call);
        return call;
    }

    @Override
    public String getType() {
        return "formula";
    }
}
