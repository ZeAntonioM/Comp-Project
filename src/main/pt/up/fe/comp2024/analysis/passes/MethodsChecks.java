package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.analysis.Utils;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.sql.SQLOutput;
import java.util.*;


public class MethodsChecks extends AnalysisVisitor {

    private final Map<String, JmmNode> fields = new HashMap<>();

    @Override
    public void buildVisitor() {
        addVisit(Kind.MEMBER_CALL_EXPR, this::visitMemberCallExpr);
    }




    private Void visitMemberCallExpr(JmmNode memberCallExpr, SymbolTable table) {

        var className = table.getClassName();
        var superClass = table.getSuper();
        var assigneeType = memberCallExpr.getChildren().get(0).get("type");


        var importSet = Utils.getImportSet(table);

        if (!Objects.equals(memberCallExpr.get("type"), "invalid")) {
            return null;
        }

        if (assigneeType.equals("self")) {
            assigneeType = className;
        }


        if (superClass.isEmpty() || !importSet.contains(superClass) || !Objects.equals(assigneeType, className)) {
            if (!importSet.contains(assigneeType)) {
                var message = String.format("Method %s not found", memberCallExpr.get("name"));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(memberCallExpr),
                        NodeUtils.getColumn(memberCallExpr),
                        message,
                        null
                ));
                return null;
            }
            memberCallExpr.put("type", assigneeType);
            return null;
        }

        // Assume the method is defined in the superclass
        memberCallExpr.put("type", superClass);

        return null;
    }

}
