package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;


public class ExpressionConditionTypeCheck extends AnalysisVisitor {


    @Override
    public void buildVisitor() {
        addVisit(Kind.IF_ELSE_STMT, this::visitIfElseStmt);
        addVisit(Kind.WHILE_STMT, this::visitWhileStmt);

    }

    private Void visitIfElseStmt(JmmNode ifElseStmt, SymbolTable table) {
        var condition = ifElseStmt.getChildren().get(0);
        if(!condition.get("type").equals("boolean")){
            var message = "Condition type must be a boolean expression.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(ifElseStmt),
                    NodeUtils.getColumn(ifElseStmt),
                    message,
                    null
            ));
        } else {
            ifElseStmt.put("type", condition.get("type"));
        }

        return null;
    }

    private Void visitWhileStmt(JmmNode whileStmt, SymbolTable table) {
        var condition = whileStmt.getChildren().get(0);
        if(!condition.get("type").equals("boolean")){
            var message = "Condition type must be a boolean expression.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(whileStmt),
                    NodeUtils.getColumn(whileStmt),
                    message,
                    null
            ));
        } else {
            whileStmt.put("type", condition.get("type"));
        }

        return null;
    }
}
