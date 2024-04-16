package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.Objects;

public class ThisOperationsCheck extends AnalysisVisitor {

    private String currentMethodsStatic;

    @Override
    public void buildVisitor() {
        addVisit(Kind.SELF_EXPR, this::visitSelfExpr);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }

    private Void visitMethodDecl(JmmNode methodDecl, SymbolTable table) {
        currentMethodsStatic = methodDecl.get("isStatic");
        return null;
    }


    private Void visitSelfExpr(JmmNode selfExpr, SymbolTable table) {

        try{
            selfExpr.get("name");
        }catch(Exception e){
            selfExpr.put("name", "this");
        }

        if(Objects.equals(currentMethodsStatic, "true")){
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(selfExpr),
                    NodeUtils.getColumn(selfExpr),
                    "Cannot use 'this' in a static method",
                    null
            ));
        }

        return null;

    }

}
