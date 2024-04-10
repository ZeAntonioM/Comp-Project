package pt.up.fe.comp2024.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;

public class Utils {

    public static String getOperandType(String operandName, SymbolTable table) {
        // Check if the operand is a field
        for (Symbol field : table.getFields()) {
            if (field.getName().equals(operandName)) {
                return field.getType().getName() + (field.getType().isArray() ? "[]" : "");
            }
        }

        // Check if the operand is a parameter or local variable of the current method
        for (String method : table.getMethods()) {
            for (Symbol parameter : table.getParameters(method)) {
                if (parameter.getName().equals(operandName)) {
                    return parameter.getType().getName() + (parameter.getType().isArray() ? "[]" : "");
                }
            }
            for (Symbol localVariable : table.getLocalVariables(method)) {
                if (localVariable.getName().equals(operandName)) {
                    return localVariable.getType().getName() + (localVariable.getType().isArray() ? "[]" : "");
                }
            }
        }

        return null;
    }
}