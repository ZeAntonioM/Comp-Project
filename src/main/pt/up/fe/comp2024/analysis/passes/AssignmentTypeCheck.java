package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

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

        if (valueType.equals("self")) {
            valueType = className;
        }

        if (assigneeType.equals("self")) {
            var message = "Cannot assign to 'this'";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    message,
                    null
            ));

            return null;
        }

        //System.out.println(value.getChildren());

        if (Objects.equals(assigneeType, valueType)
                || imports.contains(valueType) && imports.contains(assigneeType)
                || (!superClass.isEmpty() && superClass.equals(valueType) && imports.contains(valueType))
                || (imports.contains(valueType))
                || (!superClass.isEmpty() && superClass.equals(assigneeType) && imports.contains(assigneeType))
        ) {
            assignStmt.put("type", assigneeType);
        } else {
            var message = "Type mismatch in assignment statement";
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
