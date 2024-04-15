package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsSystem;

import java.util.Objects;

public class AssignmentTypeCheck extends AnalysisVisitor {


    @Override
    public void buildVisitor() {
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
    }


    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        var assignee = assignStmt.getChildren().get(0);
        var value = assignStmt.getChildren().get(1);

        var assigneeType = assignee.get("type");
        var valueType = value.get("type");

        var className = table.getClassName();
        var superClass = table.getSuper();
        var imports = table.getImports();


        if (!Objects.equals(assigneeType, valueType) && !(valueType.equals(className) && assigneeType.equals(superClass)) && !imports.contains(valueType)) {
            var message = String.format("Cannot assign a value of type '%s' to a variable of type '%s'", valueType, assigneeType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    message,
                    null
            ));
        }

        return null;
    }

}
