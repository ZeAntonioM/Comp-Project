package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.sql.SQLOutput;
import java.util.HashSet;
import java.util.Objects;

public class OperationTypesCheck extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    }


    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        var leftOperand = binaryExpr.getChildren().get(0);
        var rightOperand = binaryExpr.getChildren().get(1);
        var importSet = new HashSet<>();
        for (var i : table.getImports()){
            var pathParts = i.split("\\.");
            importSet.add(pathParts[pathParts.length - 1]);
        }

        var leftType = leftOperand.get("type");
        var rightType = rightOperand.get("type");


        var operator = binaryExpr.get("op");

        var expectedType = switch (operator) {
            case "+", "-", "*", "/", "<", ">" -> "int";
            case "&&" -> "boolean";
            default -> throw new IllegalArgumentException("Unknown operator: " + operator);
        };

        if (importSet.contains(leftType) && leftOperand.getKind().equals(Kind.MEMBER_CALL_EXPR.toString())) {
            leftType = expectedType;
        }

        if (importSet.contains(rightType) && rightOperand.getKind().equals(Kind.MEMBER_CALL_EXPR.toString())) {
            rightType = expectedType;
        }

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

        if (binaryExpr.get("type").equals("invalid") && (operator.equals(">") || operator.equals("<"))){
            binaryExpr.put("type", "boolean");
        } else {
            binaryExpr.put("type", expectedType);
        }


        return null;
    }

}
