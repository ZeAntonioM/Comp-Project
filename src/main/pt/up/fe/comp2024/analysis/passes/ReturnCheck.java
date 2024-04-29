package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.analysis.Utils;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.Objects;

public class ReturnCheck extends AnalysisVisitor {

    private String currentMethod;
    private String returnType;

    @Override
    public void buildVisitor() {
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }

    private Void visitMethodDecl(JmmNode methodDecl, SymbolTable table) {
        currentMethod = methodDecl.get("name");
        var returnNode = table.getReturnType(currentMethod);
        returnType = returnNode.getName();

        if (returnNode.isArray()){
            returnType = "int[]";
        }

        if (methodDecl.getChildren().isEmpty()) {
            return null;
        }
        var returnChild = methodDecl.getJmmChild(methodDecl.getChildren().size() - 1);

        if (!returnType.equals("void") && !returnChild.getKind().equals(Kind.RETURN_STMT.toString())) {

            var message = String.format("Method %s has a return type of %s, but there is no return", currentMethod, returnType);
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
        var returnStmtType = returnStmt.getChildren().get(0).get("type");
        var superClass = table.getSuper();
        var imports = Utils.getImports(table);

        if (!returnStmtType.equals(returnType) && !(returnStmtType.equals(superClass) || imports.contains(returnStmtType))){
            var message = String.format("Method %s has a return type of %s, but the call is being assigned to a %s", currentMethod, returnType, returnStmtType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(returnStmt),
                    NodeUtils.getColumn(returnStmt),
                    message,
                    null
            ));
        }



        return null;
    }

}
