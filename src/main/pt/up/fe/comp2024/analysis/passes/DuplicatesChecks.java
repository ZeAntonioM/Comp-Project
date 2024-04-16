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
import java.util.Objects;
import java.util.Set;


public class DuplicatesChecks extends AnalysisVisitor {


    @Override
    protected void buildVisitor() {
        addVisit(Kind.CLASS_DECL_RULE, this::visitClassFields);
        addVisit(Kind.METHOD_DECL, this::visitMethods);
        addVisit(Kind.PROGRAM, this::visitImport);
    }

    private Void visitImport(JmmNode program, SymbolTable table) {
        HashSet<String> uniqueImports = new HashSet<>();
        List<String> imports = table.getImports();

        for (String importName : imports) {
            if (!uniqueImports.add(importName)) {
                var message = String.format("Duplicate import %s", importName);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(program),
                        NodeUtils.getColumn(program),
                        message,
                        null
                ));
            }
        }



        return null;
    }

    private Void visitClassFields(JmmNode node, SymbolTable table) {
        Set<String> set = new HashSet<>();
        for (var field : node.getChildren(Kind.VAR_DECL)) {
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
            else set.add(varRef.get("name"));
        }
        return null;
    }

}
