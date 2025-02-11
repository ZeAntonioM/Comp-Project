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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public class ParamChecks extends AnalysisVisitor {

    private String currentMethod;
    @Override
    public void buildVisitor() {
        addVisit(Kind.MEMBER_CALL_EXPR, this::visitMemberCallExpr);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodCall);
    }

    private Void visitMethodCall(JmmNode methodDecl, SymbolTable table) {
        currentMethod = methodDecl.get("name");
        int idx =1 ;
        for (var param : methodDecl.getChildren(Kind.PARAM_DECL)){
            var type = param.getChild(0);
            if (type.get("isVararg").equals("true") && idx != methodDecl.getChildren(Kind.PARAM_DECL).size() ){
                var message = String.format("Method %s has a VarArg param that isn't the last element", methodDecl.get("name"));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(methodDecl),
                        NodeUtils.getColumn(methodDecl),
                        message,
                        null
                ));
            }
            idx ++;
        }
        return null;
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
        var importSet = Utils.getImports(table);

        var isUnknown = importSet.contains(obj);
        var superClass = table.getSuper();

        if (importSet.contains(type) || superClass.equals(type) || type.equals("invalid")) {
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
            compareArgParamTypes(memberCallExpr, params, args, method, varargCount, table);
        } else if (args.size() != params.size() && !isUnknown) {
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
            compareArgParamTypes(memberCallExpr, params, args, method, varargCount, table);
        }

        return null;
    }

    private void compareArgParamTypes(JmmNode memberCallExpr, List<Symbol> params, List<JmmNode> args, String method, int varargCount, SymbolTable table) {
        for (int i = 0; i < params.size()-varargCount; i++) {
            var paramType = params.get(i).getType().getName();

            if (paramType.equals("int") && params.get(i).getType().isArray()) {
                paramType = "int[]";
            }
            var argType = args.get(i).get("type");

            if (argType.equals("self")) {
                argType = table.getClassName();
            }

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

        String varargType = null;
        boolean isArray = false;
        for (int i = params.size()-varargCount; i < args.size(); i++) {
            var argType = args.get(i).get("type");
            if (varargType == null) {
                varargType = argType;
                isArray = argType.equals("int[]");
            } else if (!varargType.equals(argType)) {
                var message = String.format("Vararg arguments in method %S are not all of the same type", method);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(memberCallExpr),
                        NodeUtils.getColumn(memberCallExpr),
                        message,
                        null
                ));
                break;
            } else if (isArray) {
                var message = String.format("Vararg arguments on %s cant have more than one int[]", method);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(memberCallExpr),
                        NodeUtils.getColumn(memberCallExpr),
                        message,
                        null
                ));
                break;

            }
        }
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {
        var var = varDecl.getChildren().get(0).get("isVararg");
        var varRefName = varDecl.get("name");
        if (table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(varRefName))) {
            var message = "Variable name is the same as a parameter name";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(varDecl),
                    NodeUtils.getColumn(varDecl),
                    message,
                    null
            ));
        }
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
