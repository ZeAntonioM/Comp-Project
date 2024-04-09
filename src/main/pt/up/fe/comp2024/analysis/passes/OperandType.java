package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
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

        System.out.println("Visit Binary Expr");


        // Get the left and right operands
        var leftOperand = binaryExpr.getChildren().get(0);
        var rightOperand = binaryExpr.getChildren().get(1);

        // Get the names of the operands
        var leftName = leftOperand.get("name");
        var rightName = rightOperand.get("name");

        // Get the types of the operands
        var leftType = getOperandType(leftName, table);
        var rightType = getOperandType(rightName, table);


        // Get the operator
        var operator = binaryExpr.get("op");

        System.out.println(leftType + " " + leftName + " " + operator + " " + rightType + " " +rightName);


        var expectedType = switch (operator) {
            case "+", "-", "*", "/", "<", ">" -> "int";
            case "&&" -> "boolean";
            default -> throw new IllegalArgumentException("Unknown operator: " + operator);
        };

        checkOperandType(binaryExpr, operator, leftType, expectedType);
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


    private String getOperandType(String operandName, SymbolTable table) {
        // Check if the operand is a field
        for (Symbol field : table.getFields()) {
            if (field.getName().equals(operandName)) {
                return field.getType().getName() + (field.getType().isArray() ? "[]" : "");
            }
        }

        // Check if the operand is a parameter or local variable of the current method
        for (String method : table.getMethods()) {
            for (Symbol parameter : table.getParameters(method)) {
                if (parameter.getName().equals(operandName)) {
                    return parameter.getType().getName() + (parameter.getType().isArray() ? "[]" : "");
                }
            }
            for (Symbol localVariable : table.getLocalVariables(method)) {
                if (localVariable.getName().equals(operandName)) {
                    return localVariable.getType().getName() + (localVariable.getType().isArray() ? "[]" : "");
                }
            }
        }

        // If the operand is not a field, parameter, or local variable, return null
        return null;
    }

}
