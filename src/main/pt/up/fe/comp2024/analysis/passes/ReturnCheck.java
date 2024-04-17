package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.Objects;

public class ReturnCheck extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }

    private Void visitMethodDecl(JmmNode methodDecl, SymbolTable table) {
        currentMethod = methodDecl.get("name");
        var returnNode = table.getReturnType(currentMethod);
        var returnType = returnNode.getName();

        if (returnNode.isArray()){
            returnType = "int[]";
        }
        if (methodDecl.getChildren().isEmpty()) {
            return null;
        }
        var returnChild = methodDecl.getJmmChild(methodDecl.getChildren().size() - 1);
        var childType = returnChild.get("type");
        var superClass = table.getSuper();


        if (!returnType.equals(childType) && !returnType.equals("void") && !(childType.equals(superClass) || table.getImports().contains(childType))) {

            if(childType.isEmpty()){
                childType = "invalid";
            }


            var message = String.format("Method %s has a return type of %s, but the call is being assigned to a %s variable", currentMethod, returnType, childType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(returnChild),
                    NodeUtils.getColumn(returnChild),
                    message,
                    null
            ));
        }

        return null;
    }


    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table) {
        var returnType = table.getReturnType(currentMethod).getName();
        var returnStmtType = returnStmt.get("type");

        var message = String.format("Method %s has a return type of %s, but the call is being assigned to a %s", currentMethod, returnType, returnStmtType);
        addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(returnStmt),
                NodeUtils.getColumn(returnStmt),
                message,
                null
        ));

        return null;
    }

}
