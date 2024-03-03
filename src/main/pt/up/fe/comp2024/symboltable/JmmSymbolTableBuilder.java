package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AllNodesJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pt.up.fe.comp2024.ast.Kind.*;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {

        var classDecl = root.getJmmChild(0);
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");

        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(className, methods, returnTypes, params, locals);
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();

        List<JmmNode> children = classDecl.getChildren(METHOD_DECL);

        for (JmmNode method : children) {
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
            List<JmmNode> paramsTypeNodes = method.getChildren(TYPE);
            List<String> paramsNames = method.getObjectAsList("args", String.class);
            // remove the first element of paramsTypeNodes, which is the return type
            paramsTypeNodes.remove(0);

            for (int i = 0; i < paramsTypeNodes.size(); i++) {
                String type = paramsTypeNodes.get(i).get("name");
                boolean isArray = paramsTypeNodes.get(i).get("isArray").equals("true");

                String name = paramsNames.get(i);

                map.put(method.get("name"), Arrays.asList(new Symbol(new Type(type, isArray), name)));
            }
        }

        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {

        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }


    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        // TODO: Simple implementation that needs to be expanded

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(intType, varDecl.get("name")))
                .toList();
    }

}
