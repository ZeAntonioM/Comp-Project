package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";
    private static final String BOOLEAN_TYPE_NAME = "boolean";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case BOOL_EXPR, NEG_EXPR -> new Type(BOOLEAN_TYPE_NAME,false);
            default -> new Type(expr.get("name"), false);
        };

        return type;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        // TODO: Simple implementation that needs to be expanded

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*", "-", "/" -> new Type(INT_TYPE_NAME, false);
            case "<", ">", "&&" -> new Type(BOOLEAN_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }


    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        // TODO: Simple implementation that needs to be expanded
        var parent = varRefExpr.getParent();

        while(parent != null && (!Kind.METHOD_DECL.check(parent) &&  !Kind.CLASS_DECL_RULE.check(parent))) {
            parent = parent.getParent();
        }
        var methodName = parent.get("name");
        var params = table.getParameters(methodName);
        var varName = varRefExpr.get("name");

        for (var param : params) {
            if (param.getName().equals(varName)) {
                return param.getType();
            }
        }

        var locals = table.getLocalVariables(methodName);

        for (var local : locals) {
            if (local.getName().equals(varName)) {
                return local.getType();
            }
        }

        var fields = table.getFields();

        for (var field : fields) {
            if (field.getName().equals(varName)) {
                return field.getType();
            }
        }

        return new Type("", false);
       // throw new RuntimeException("Variable '" + varName + "' not found in method '" + methodName + "'");
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: Simple implementation that needs to be expanded
        return sourceType.getName().equals(destinationType.getName());
    }
}
