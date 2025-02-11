package pt.up.fe.comp2024.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.HashSet;

public class Utils {

    public static String getType(JmmNode operand, SymbolTable table, String method) {

        String operandName = operand.get("name");
        for (Symbol parameter : table.getParameters(method)) {
            if (parameter.getName().equals(operandName)) {
                return parameter.getType().getName() + (parameter.getType().isArray() ? "[]" : "");
            }
        }

        // Check if the operand is a local variable of the current method
        for (Symbol localVariable : table.getLocalVariables(method)) {
            if (localVariable.getName().equals(operandName)) {
                return localVariable.getType().getName() + (localVariable.getType().isArray() ? "[]" : "");
            }
        }

        // If not found, check if the operand is a field
        for (Symbol field : table.getFields()) {
            if (field.getName().equals(operandName)) {
                return field.getType().getName() + (field.getType().isArray() ? "[]" : "");
            }
        }

        if (table.getImports().contains(operandName)) {
            return operandName;
        }

        var importSet = Utils.getImports(table);

        if (importSet.contains(operandName)) {
            return operandName;
        }

        return "";

    }

    public static HashSet<String> getImports(SymbolTable table) {
        var importSet = new HashSet<String>();
        for (var i : table.getImports()){
            var pathParts = i.split("\\.");
            importSet.add(pathParts[pathParts.length - 1]);
        }
        return importSet;
    }
}