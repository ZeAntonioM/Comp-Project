package pt.up.fe.comp2024.analysis.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;

import java.util.List;


public class ConstantFolding extends AnalysisVisitor {

    public boolean modified = false;

    public void optimize(JmmNode node, SymbolTable table) {
        modified = false;
        visit(node, table);
    }


    @Override
    public void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    }


    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        var left = binaryExpr.getChildren().get(0);
        var right = binaryExpr.getChildren().get(1);

        if (left.getKind().equals(Kind.INTEGER_LITERAL.toString()) && right.getKind().equals(Kind.INTEGER_LITERAL.toString())) {
            int leftValue = Integer.parseInt(left.get("value"));
            int rightValue = Integer.parseInt(right.get("value"));

            String op = binaryExpr.get("op");
            if (!List.of("+", "-", "*", "/").contains(op)) {
                return null;
            }

            int result = switch (op) {
                case "+" -> leftValue + rightValue;
                case "-" -> leftValue - rightValue;
                case "*" -> leftValue * rightValue;
                case "/" -> leftValue / rightValue;
                default -> throw new IllegalStateException("Unexpected value: " + op);
            };

            var newNode = new JmmNodeImpl(Kind.INTEGER_LITERAL.toString());
            newNode.put("value", Integer.toString(result));
            binaryExpr.replace(newNode);
            modified = true;
        }

        return null;
    }


}

