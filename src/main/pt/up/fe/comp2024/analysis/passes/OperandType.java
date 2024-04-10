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

import java.util.Arrays;
import java.util.Objects;

/**
 * Checks if the operands of an operation have types compatible with the operation
 *
 */
public class OperandType extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        // Get the left and right operands
        var leftOperand = binaryExpr.getChildren().get(0);
        var rightOperand = binaryExpr.getChildren().get(1);

        // Initialize the types of the operands
        String leftType = Utils.getOperandType(leftOperand, table);
        String rightType = Utils.getOperandType(rightOperand, table);

        leftOperand.put("type", leftType);
        rightOperand.put("type", rightType);

        // Get the operator
        var operator = binaryExpr.get("op");

        var expectedType = switch (operator) {
            case "+", "-", "*", "/", "<", ">" -> "int";
            case "&&" -> "boolean";
            default -> throw new IllegalArgumentException("Unknown operator: " + operator);
        };

        binaryExpr.put("type", expectedType);

        if (!Objects.equals(leftType, expectedType) || !Objects.equals(rightType, expectedType)) {
            var message = String.format("Operator '%s' expects operands of type '%s', but got '%s' and '%s'", operator, expectedType, leftType, rightType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(binaryExpr),
                    NodeUtils.getColumn(binaryExpr),
                    message,
                    null
            ));
        }

        return null;
    }

}
