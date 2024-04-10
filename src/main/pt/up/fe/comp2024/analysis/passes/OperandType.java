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
        var children = binaryExpr.getChildren();

        // Get the names of the operands
        var leftName = leftOperand.get("name");
        var rightName = rightOperand.get("name");

        // Get the types of the operands
        var leftType = Utils.getOperandType(leftName, table);
        var rightType = Utils.getOperandType(rightName, table);

        leftOperand.put("type", leftType);
        rightOperand.put("type", rightType);


        // Get the operator
        var operator = binaryExpr.get("op");

        System.out.println(leftType + " " + leftName + " " + operator + " " + rightType + " " +rightName);


        var expectedType = switch (operator) {
            case "+", "-", "*", "/", "<", ">" -> "int";
            case "&&" -> "boolean";
            default -> throw new IllegalArgumentException("Unknown operator: " + operator);
        };

        binaryExpr.put("type", expectedType);

        System.out.println("binaryExpr" + " " + binaryExpr);
        System.out.println("leftOperand" + " " + leftOperand);
        System.out.println("rightOperand" + " " + rightOperand);


        assert leftType != null;
        checkOperandType(binaryExpr, operator, leftType, expectedType);

        assert rightType != null;
        checkOperandType(binaryExpr, operator, rightType, expectedType);


        return null;
    }

    private void checkOperandType(JmmNode binaryExpr, String operator, String operandType, String expectedType) {
        if (!operandType.equals(expectedType)) {
            var message = String.format("Operator '%s' expects operands of type '%s', but got '%s'", operator, operandType, expectedType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(binaryExpr),
                    NodeUtils.getColumn(binaryExpr),
                    message,
                    null
            ));
        }

    }

}
