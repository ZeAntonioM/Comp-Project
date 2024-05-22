package pt.up.fe.comp2024.analysis.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pt.up.fe.comp2024.analysis.AnalysisVisitor;


public class ConstantPropagation extends AnalysisVisitor {

    private final Map<String, JmmNode > constantVariables = new HashMap<>();
    private final Set<String> loopConditionVars = new HashSet<>();
    public boolean modified = false;

    public void optimize(JmmNode node, SymbolTable table) {
        modified = false;
        visit(node, table);
    }


    @Override
    public void buildVisitor() {
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.WHILE_STMT, this::visitWhileStmt);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
    }


    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        var assignee = assignStmt.getChildren().get(0);
        var assignExpr = assignStmt.getChildren().get(1);


        if (assignExpr.getKind().equals(Kind.INTEGER_LITERAL.toString())) {
            constantVariables.put(assignee.get("name"), assignExpr);
            loopConditionVars.add(assignee.get("name"));
        }

        return null;
    }

    private Void visitWhileStmt(JmmNode whileStmt, SymbolTable table) {
        var condition = whileStmt.getChildren().get(0);
        var body = whileStmt.getChildren().get(1);

        for (var var : condition.getDescendants()) {
            if (var.getKind().equals(Kind.VAR_REF_EXPR.toString())) {
                loopConditionVars.add(var.get("name"));
            }
        }

        for (var var : body.getDescendants(Kind.VAR_REF_EXPR)) {
            loopConditionVars.remove(var.get("name"));
        }

        return null;
    }


    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        String varName = varRefExpr.get("name");


        if (constantVariables.containsKey(varName) && loopConditionVars.contains(varName)) {
            if(varRefExpr.getParent().getKind().equals(Kind.ASSIGN_STMT.toString())) {
                return null;
            }

            varRefExpr.replace(constantVariables.get(varName));
            modified = true;
        }

        return null;
    }

}

