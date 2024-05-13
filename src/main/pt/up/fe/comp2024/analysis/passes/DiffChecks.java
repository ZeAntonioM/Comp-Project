package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class DiffChecks extends AnalysisVisitor {

    private final Set<String> fields = new HashSet<>();
    @Override
    protected void buildVisitor() {
        addVisit(Kind.CLASS_DECL_RULE, this::visitClassFields);
        addVisit(Kind.METHOD_DECL, this::visitMethods);
        addVisit(Kind.PROGRAM, this::visitImport);
    }

    private Void visitImport(JmmNode program, SymbolTable table) {
        Set<String> set = new HashSet<>();
        for (var imported_path: program.getChildren(Kind.IMPORT_DECL)) {
            String pathString = imported_path.get("name");
            String[] pathParts = pathString.substring(1, pathString.length() - 1).split(", ");
            String className = pathParts[pathParts.length - 1];
            if(set.contains(className)){
                var message = String.format("Duplicated import %s", className);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(imported_path),
                        NodeUtils.getColumn(imported_path),
                        message,
                        null
                ));
            }
            else set.add(className);
        }

        return null;
    }

    private Void visitClassFields(JmmNode node, SymbolTable table) {
        Set<String> set = new HashSet<>();
        for (var field : node.getChildren(Kind.VAR_DECL)) {
            fields.add(field.get("name"));
            if (set.contains(field.get("name"))) {
                var message = String.format("Duplicated field %s", field.get("name"));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(field),
                        NodeUtils.getColumn(field),
                        message,
                        null
                ));
            } else set.add(field.get("name"));
        }
        set = new HashSet<>();
        for (var method : node.getChildren(Kind.METHOD_DECL)) {
            if (set.contains(method.get("name"))) {
                var message = String.format("Duplicated method %s", method.get("name"));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null
                ));
            } else set.add(method.get("name"));
        }
        return null;
    }


    private Void visitMethods(JmmNode node, SymbolTable table){
        Set<String> set = new HashSet<>();
        Set<String> localVars = new HashSet<>();
        for (var param: node.getChildren(Kind.PARAM_DECL)) {
            if(set.contains(param.get("name"))){
                var message = String.format("Duplicated parameter %s", param.get("name"));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(param),
                        NodeUtils.getColumn(param),
                        message,
                        null
                ));
            }
            else set.add(param.get("name"));
        }
        set = new HashSet<>();
        for (var varRef: node.getChildren(Kind.VAR_DECL)) {
            if(set.contains(varRef.get("name"))){
                var message = String.format("Duplicated variable %s", varRef.get("name"));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varRef),
                        NodeUtils.getColumn(varRef),
                        message,
                        null
                ));
            }
            else {
                set.add(varRef.get("name"));
                localVars.add(varRef.get("name"));
            }

        }

        List<JmmNode> varReferences = node.getDescendants(Kind.VAR_REF_EXPR);
        for (JmmNode varRef : varReferences) {
            if (fields.contains(varRef.get("name")) && node.get("isStatic").equals("true") && !localVars.contains(varRef.get("name"))) {
                var message = String.format("Variable %s is a field", varRef.get("name"));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(varRef),
                        NodeUtils.getColumn(varRef),
                        message,
                        null
                ));
            }
        }

        return null;
    }

}
