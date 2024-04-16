package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.analysis.Utils;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.List;


public class ParamChecks extends AnalysisVisitor {


    @Override
    public void buildVisitor() {
        addVisit(Kind.MEMBER_CALL_EXPR, this::visitMemberCallExpr);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
    }

    private Void visitMemberCallExpr(JmmNode memberCallExpr, SymbolTable table) {


        String obj;

        try {
            obj = memberCallExpr.getChildren().get(0).get("name");
        } catch (Exception e) {
            obj = table.getClassName();
        }

        var type = memberCallExpr.get("type");
        var method = memberCallExpr.get("name");
        var imports = table.getImports();
        var isUnkown = imports.contains(obj);
        var superClass = table.getSuper();

        if (imports.contains(type) || superClass.equals(type) || type.equals("invalid")) {
            return null;
        }

        var params = table.getParameters(method);
        var args = memberCallExpr.getChildren().subList(1, memberCallExpr.getChildren().size());


        int varargCount = 0;
        boolean varargIsLast = false;

        for (int i = 0; i < params.size(); i++) {
            var param = params.get(i);
            if (param.getType().getName().equals("vararg")) {
                varargCount++;
                if (i == params.size() - 1) {
                    varargIsLast = true;
                }
            }
        }

        if (varargCount > 1) {
            var message = String.format("Method %s has more than one Vararg param", method);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(memberCallExpr),
                    NodeUtils.getColumn(memberCallExpr),
                    message,
                    null
            ));
        } else if (varargCount == 1 && !varargIsLast) {
            var message = String.format("Method %s has a VarArg param that isn't the last element", method);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(memberCallExpr),
                    NodeUtils.getColumn(memberCallExpr),
                    message,
                    null
            ));
        } else if (varargCount == 1) {
            compareArgParamTypes(memberCallExpr, params, args, method, varargCount);
        } else if (args.size() != params.size() && !isUnkown) {
            var message = String.format("Method %s has %d parameters, but %d were given", method, params.size(), args.size());
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(memberCallExpr),
                    NodeUtils.getColumn(memberCallExpr),
                    message,
                    null
            ));
        }
        else {
            compareArgParamTypes(memberCallExpr, params, args, method, varargCount);
        }

        return null;
    }

    private void compareArgParamTypes(JmmNode memberCallExpr, List<Symbol> params, List<JmmNode> args, String method, int varargCount) {


        for (int i = 0; i < params.size()-varargCount; i++) {
            var paramType = params.get(i).getType().getName();

            if (paramType.equals("int") && params.get(i).getType().isArray()) {
                paramType = "int[]";
            }
            var argType = args.get(i).get("type");
            if (!paramType.equals(argType)) {
                var message = String.format("Call to method %S was made with incorrect argument types", method);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(memberCallExpr),
                        NodeUtils.getColumn(memberCallExpr),
                        message,
                        null
                ));
            }
        }
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {
        var var = varDecl.getChildren().get(0).get("isVararg");
        if (var.equals("true")){
            var message = "Vararg detected in variable declaration";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(varDecl),
                    NodeUtils.getColumn(varDecl),
                    message,
                    null
            ));
        }
        return null;
    }

}
