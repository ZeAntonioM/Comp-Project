package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import static pt.up.fe.comp2024.ast.Kind.TYPE;

public class OptUtils {
    private static int tempNumber = -1;

    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static int getNextTempNum() {

        tempNumber += 1;
        return tempNumber;
    }

    public static String toOllirType(JmmNode typeNode) {

        TYPE.checkOrThrow(typeNode);

        String typeName = typeNode.get("name");

        return toOllirType(typeName, Boolean.parseBoolean(typeNode.get("isArray")) || Boolean.parseBoolean(typeNode.get("isVararg")));
    }

    public static String toOllirType(Type type) {
        return toOllirType(type.getName(), type.isArray());
    }

    private static String toOllirType(String typeName, boolean isArray) {

        String type = "." + switch (typeName) {
            case "int":
                if (isArray) yield "array.i32";
                else yield "i32";
            case "boolean":
                yield "bool";
            case "void":
                yield "V";
            default:
                yield typeName;
        };
        return type;
    }
}
