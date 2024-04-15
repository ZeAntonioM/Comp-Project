package pt.up.fe.comp2024.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;

public class Utils {

    public static String getOperandType(JmmNode operand, SymbolTable table, String method) {

        if (operand.getKind().equals(Kind.INTEGER_LITERAL.toString())) {
            return "int";
        } else if (operand.getKind().equals(Kind.BOOL_EXPR.toString())) {
            return "boolean";
        }
        else if (operand.getKind().equals(Kind.PRECEDENT_EXPR.toString()) || operand.getKind().equals(Kind.BINARY_EXPR.toString())) {
            String type = null;
            for (JmmNode child : operand.getChildren()) {
                String childType = getOperandType(child, table, method);
                if (childType != null) {
                    if (type == null) {
                        type = childType;
                    }
                    else if (!type.equals(childType)) {
                        return null;
                    }
                }
            }
            return type;
        } else if (operand.getKind().equals(Kind.MEMBER_CALL_EXPR.toString())){
            String methodName = operand.get("name");

            // Get the return type of the method from the symbol table
            var returnType = table.getReturnType(methodName);

            // Return the name of the return type
            return returnType.getName() + (returnType.isArray() ? "[]" : "");
        }
        else if (operand.getKind().equals(Kind.ARRAY_INIT_EXPR.toString())) {
            return "int[]";
        }
        else {
            String operandName = operand.get("name");
            // Check if the operand is a parameter or local variable of the current method first
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

            if (!table.getImports().contains(operandName)) {
                return operandName;
            }
        }
        return null;
    }
}