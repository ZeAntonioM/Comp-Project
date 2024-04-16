package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.analysis.Utils;
import pt.up.fe.comp2024.ast.Kind;

import java.util.Objects;

public class NodesTypesCheck extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {

        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
        addVisit(Kind.METHOD_DECL, this::visitMethodCall);
        addVisit(Kind.ARRAY_REF_EXPR, this::visitArrayRefExpr);
        addVisit(Kind.INTEGER_LITERAL, this::visitIntegerLiteral);
        addVisit(Kind.BOOL_EXPR, this::visitBooleanLiteral);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.NEW_ARRAY_EXPR, this::visitNewArrayExpr);
        addVisit(Kind.PRECEDENT_EXPR, this::visitPrecedentExpr);
        addVisit(Kind.WHILE_STMT, this::visitWhileStmt);
        addVisit(Kind.NEW_OBJ_EXPR, this::visitNewObjectType);
        addVisit(Kind.MEMBER_CALL_EXPR, this::visitMemberCallExpr);
        addVisit(Kind.PARAM_DECL, this::visitParamDecl);
        addVisit(Kind.IF_ELSE_STMT, this::visitIfElseStmt);
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
        addVisit(Kind.SELF_EXPR, this::visitSelfExpr);
        addVisit(Kind.ARRAY_INIT_EXPR, this::visitArrayInitExpr);
        addVisit(Kind.EXPR_STMT, this::visitExprStmt);
    }

    private Void visitMethodCall(JmmNode methodCall, SymbolTable table) {
        currentMethod = methodCall.get("name");
        table.getReturnType(currentMethod);
        methodCall.put("type", table.getReturnType(currentMethod).getName());
        return null;
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        var leftOperand = binaryExpr.getChildren().get(0);
        var rightOperand = binaryExpr.getChildren().get(1);

        visit(leftOperand, table);
        visit(rightOperand, table);

        var leftType = leftOperand.get("type");
        var rightType = rightOperand.get("type");

        if (Objects.equals(leftType, rightType)) {
            binaryExpr.put("type", leftType);
        } else {
            binaryExpr.put("type", "invalid");
        }

        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        var varType = Utils.getType(varRefExpr, table, currentMethod);
        varRefExpr.put("type", varType);
        return null;
    }

    private Void visitArrayRefExpr(JmmNode arrayRefExpr, SymbolTable table) {
        var arrayVar = arrayRefExpr.getChildren().get(0);
        var index = arrayRefExpr.getChildren().get(1);

        visit(arrayVar, table);
        visit(index, table);

        var arrayVarType = arrayVar.get("type");
        var indexType = index.get("type");

        if ((Objects.equals(arrayVarType, "int[]") || Objects.equals(arrayVarType, "vararg")) && Objects.equals(indexType, "int")) {
            arrayRefExpr.put("type", "int");
        } else {
            arrayRefExpr.put("type", "invalid");
        }

        return null;
    }

    private Void visitIntegerLiteral(JmmNode integerLiteral, SymbolTable table) {
        integerLiteral.put("type", "int");
        return null;
    }

    private Void visitBooleanLiteral(JmmNode booleanLiteral, SymbolTable table) {
        booleanLiteral.put("type", "boolean");
        return null;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        var Var = assignStmt.getChildren().get(0);
        var Value = assignStmt.getChildren().get(1);

        visit(Var, table);
        visit(Value, table);

        var varType = Var.get("type");
        var valueType = Value.get("type");

        if (Objects.equals(varType, valueType)) {
            assignStmt.put("type", varType);
        } else {
            assignStmt.put("type", "invalid");
        }

        return null;
    }

    private Void visitNewArrayExpr(JmmNode newArrayExpr, SymbolTable table) {
        var size = newArrayExpr.getChildren().get(0);
        visit(size, table);

        var sizeType = size.get("type");

        if (Objects.equals(sizeType, "int")) {
            newArrayExpr.put("type", "int[]");
        } else {
            newArrayExpr.put("type", "invalid");
        }

        return null;
    }

    private Void visitPrecedentExpr(JmmNode precedentExpr, SymbolTable table) {
        var expr = precedentExpr.getChildren().get(0);
        visit(expr, table);
        precedentExpr.put("type", expr.get("type"));
        return null;
    }

    private Void visitWhileStmt(JmmNode whileStmt, SymbolTable table) {
        var condition = whileStmt.getChildren().get(0);
        visit(condition, table);
        var conditionType = condition.get("type");
        if (Objects.equals(conditionType, "boolean")) {
            whileStmt.put("type", "boolean");
        } else {
            whileStmt.put("type", "invalid");
        }
        return null;
    }

    private Void visitMemberCallExpr(JmmNode memberCallExpr, SymbolTable table) {
        var object = memberCallExpr.getChildren().get(0);
        var method = memberCallExpr.get("name");
        var methodType = table.getReturnType(method);
        visit(object, table);

        if (methodType != null) {
            memberCallExpr.put("type", methodType.getName());
        } else {
            memberCallExpr.put("type", "invalid");
        }

        return null;
    }

    private Void visitNewObjectType(JmmNode objectType, SymbolTable table) {
        objectType.put("type", objectType.get("name"));
        return null;
    }

    private Void visitParamDecl(JmmNode paramDecl, SymbolTable table) {
        var type = Utils.getType(paramDecl, table, currentMethod);
        paramDecl.put("type", type);
        return null;
    }

    private Void visitIfElseStmt(JmmNode ifElseStmt, SymbolTable table) {
        var condition = ifElseStmt.getChildren().get(0);

        visit(condition, table);

        var conditionType = condition.get("type");


        if (Objects.equals(conditionType, "boolean")) {
            ifElseStmt.put("type", conditionType);
        } else {
            ifElseStmt.put("type", "invalid");
        }

        return null;
    }

    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table) {
        var returnValue = returnStmt.getChildren().get(0);
        visit(returnValue, table);
        var returnType = returnValue.get("type");
        returnStmt.put("type", returnType);
        return null;
    }

    private Void visitSelfExpr(JmmNode selfExpr, SymbolTable table) {
        selfExpr.put("type", "self");
        return null;
    }

    private Void visitArrayInitExpr(JmmNode arrayInitExpr, SymbolTable table) {
        var hasType = false;
        for (JmmNode child : arrayInitExpr.getChildren()) {
            visit(child, table);
            var childType = child.get("type");
            if (!Objects.equals(childType, "int")) {
                arrayInitExpr.put("type", "invalid");
                hasType = true;
                break;
            }
        }


        if (!hasType) {
            arrayInitExpr.put("type", "int[]");
        }

        return null;
    }

    private Void visitExprStmt(JmmNode exprStmt, SymbolTable table) {
        var expr = exprStmt.getChildren().get(0);
        visit(expr, table);
        exprStmt.put("type", expr.get("type"));
        return null;
    }



}
