package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.analysis.Utils;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.lang.reflect.Array;

/**
 * Checks all the array operations
 *
 */
public class ArrayOperations extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        var var = assignStmt.getChildren().get(0);
        var varType = Utils.getOperandType(var, table);

        var hasArray = assignStmt.getChildren().stream().anyMatch(child -> child.getKind().equals(Kind.ARRAY_INIT_EXPR.toString()));

        if (hasArray) {
            assert varType != null;
            if (!varType.contains("[]")) {
                var message = String.format("Cannot assign an array to a non-array variable '%s'", var.get("name"));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null
                ));
            }
        }

        return null;
    }
}
