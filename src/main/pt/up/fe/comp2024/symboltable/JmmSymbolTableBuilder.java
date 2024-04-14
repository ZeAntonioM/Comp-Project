package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AllNodesJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.*;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {

        var classDecl = root.getChildren(CLASS_DECL_RULE).get(0);
        SpecsCheck.checkArgument(Kind.CLASS_DECL_RULE.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");
        boolean hasSuperClass = classDecl.get("hasSuperClass").equals("true");
        String superClass = hasSuperClass ? classDecl.get("superclass") : "";

        var imports = buildImports(root);
        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);
        var fields = buildFields(classDecl);


        return new JmmSymbolTable(className, superClass, imports, methods, returnTypes, params, locals, fields);
    }

    private static List<String> buildImports(JmmNode root) {
        List<String> imports= new ArrayList<>();

        List<List<String>> listNames = root.getChildren(IMPORT_DECL).stream()
                .map(importDecl -> importDecl.getObjectAsList("name", String.class)).toList();

        for (List<String> list : listNames) {
            String importName = String.join(".", list);
            imports.add(importName);
        }

        return imports;
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {
        List<Symbol> fields = new ArrayList<>();
        List<JmmNode> children = classDecl.getChildren(VAR_DECL);

        for (JmmNode varDecl: children) {
            JmmNode typeNode = varDecl.getChildren(TYPE).get(0);
            String type = typeNode.get("name");
            boolean isArray = typeNode.get("isArray").equals("true");
            String name = varDecl.get("name");

            fields.add(new Symbol(new Type(type, isArray), name));
        }
        return fields;
    }


    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();

        List<JmmNode> children = classDecl.getChildren(METHOD_DECL);

        for (JmmNode method : children) {
            boolean isMain = method.get("name").equals("main");

            if (isMain) {
                map.put(method.get("name"), new Type("void", false));
                continue;
            }

            JmmNode typeNode = method.getChildren(TYPE).get(0);
            String type = typeNode.get("name");
            boolean isArray = typeNode.get("isArray").equals("true");

            map.put(method.get("name"), new Type(type, isArray));

        }
        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        List<JmmNode> children = classDecl.getChildren(METHOD_DECL);

        for (JmmNode method : children) {

            String name = method.get("name");

            List<Symbol> params = new ArrayList<>();

            if (name.equals("main")) {
                params.add(new Symbol(new Type("String", true), "args"));
                map.put(name, params);
                continue;
            }

            List<JmmNode> paramsNodes = method.getChildren(PARAM_DECL);

            for (int i = 0; i < paramsNodes.size(); i++) {
                JmmNode paramNode = paramsNodes.get(i);
                String paramName = paramNode.get("name");

                JmmNode typeNode = paramNode.getChildren(TYPE).get(0);

                String type = typeNode.get("name");
                boolean isArray = typeNode.get("isArray").equals("true");
                boolean isVararg = typeNode.get("isVararg").equals("true");

                if (isVararg && !isArray && type.equals("int") && i == paramsNodes.size() - 1) {
                    params.add(new Symbol(new Type("vararg", false), paramName));
                    continue;
                }

                params.add(new Symbol(new Type(type, isArray), paramName));
            }

            map.put(name, params);
        }

        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        List<JmmNode> children = classDecl.getChildren(METHOD_DECL);

        for (JmmNode method : children) {

            String name = method.get("name");

            List<Symbol> locals = getLocalsList(method);

            map.put(name, locals);
        }
        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {
        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }


    private static List<Symbol> getLocalsList(JmmNode methodDecl) {

        List<Symbol> locals = new ArrayList<>();

        List<JmmNode> children = methodDecl.getChildren(VAR_DECL);

        for (JmmNode varDecl: children) {

            JmmNode typeNode = varDecl.getChildren(TYPE).get(0);
            String type = typeNode.get("name");
            boolean isArray = typeNode.get("isArray").equals("true");

            locals.add(new Symbol(new Type(type, isArray), varDecl.get("name")));

        }
        return locals;

    }

}
