package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

/**
 * Checks all the array operations
 *
 */
public class ArrayOperations extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.ARRAY_REF_EXPR, this::visitArrayRefExpr);
    }

    private Void visitArrayRefExpr(JmmNode arrayRefExpr, SymbolTable table) {

        // Get the array
        var array = arrayRefExpr.getChildren().get(0);

        // Get the index
        var index = arrayRefExpr.getChildren().get(1);

        // Get the type of the array
        var arrayType = array.get("type");
        var indexType = index.get("type");

        if (!arrayType.equals("array")) {
            var message = String.format("Operator '[]' expects an array, but got '%s'", arrayType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayRefExpr),
                    NodeUtils.getColumn(arrayRefExpr),
                    message,
                    null
            ));
        }

        if (!indexType.equals("int")) {
            var message = String.format("Operator '[]' expects an int index, but got '%s'", indexType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayRefExpr),
                    NodeUtils.getColumn(arrayRefExpr),
                    message,
                    null
            ));
        }


        return null;
    }
}
