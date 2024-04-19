package pt.up.fe.comp2024.optimization;

import org.junit.Test;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;

import javax.print.DocFlavor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(MEMBER_CALL_EXPR, this::visitMemberCallExpr);
        addVisit(PRECEDENT_EXPR, this::visitPrecedentExpr);
        addVisit(BOOL_EXPR, this::visitBoolExpr);
        addVisit(NEG_EXPR, this::visitNegExpr);
        addVisit(SELF_EXPR, this::visitSelfExpr);
        addVisit(NEW_OBJ_EXPR, this::visitNewObjExpr);

        setDefaultVisit(this::defaultVisit);
    }
    public String getClosestOccurrenceVariable(String variableName, String methodSignature) {
        if (variableName.equals("this")){
            return "class";
        }

        for (Symbol s: table.getLocalVariables(methodSignature)){
            if (s.getName().equals(variableName)){
                return "local";
            }
        }
        for (Symbol s: table.getParameters(methodSignature)){
            if (s.getName().equals(variableName)){
                return "param";
            }
        }

        for (Symbol s: table.getFields()){
            if (s.getName().equals(variableName)){
                return "field";
            }
        }
/*
        for (String s : table.getMethods()){
            if (s.equals(variableName)){
                return "method";
            }
        }
*/
        for (String s: table.getImports()){
            var split = s.split("\\.");
            for (String s1: split){
                if (s1.equals(variableName)){
                    return "import";
                }
            }
        }

        return "not found";
    }

    private OllirExprResult visitNewObjExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();

        var tmp = OptUtils.getTemp();
        var type = OptUtils.toOllirType(TypeUtils.getExprType(node, table));

        computation.append(tmp).append(type).append(SPACE).append(ASSIGN).append(type)
                .append(SPACE).append("new").append("(").append(node.get("name")).append(")")
                .append(type).append(END_STMT);

        computation.append("invokespecial(").append(tmp).append(type).append(", \"<init>\").V").append(END_STMT);

        return new OllirExprResult(tmp + type, computation.toString());
    }
// TODO : IF ALL ELSE FAILS, CHANGE THIS TO ADD CLASSNAME AS TYPE
    private OllirExprResult visitSelfExpr(JmmNode node, Void unused) {
        return new OllirExprResult("this." + table.getClassName());
    }

    private OllirExprResult visitNegExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        var child = visit(node.getJmmChild(0));

        computation.append(child.getComputation());

        computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE)
                .append("!").append(resOllirType).append(SPACE).append(child.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);

    }

    private OllirExprResult visitBoolExpr(JmmNode node, Void unused) {
        if (node.get("bool").equals("true")){
            return new OllirExprResult("1.bool");
        }
        else {
            return new OllirExprResult("0.bool");
        }
    }

    //TODO delete this
    private OllirExprResult visitPrecedentExpr(JmmNode node, Void unused) {
        var code = new StringBuilder();
        var computation = new StringBuilder();

        for (var child : node.getChildren()) {
            var childResult = visit(child);
            code.append(childResult.getCode());
            computation.append(childResult.getComputation());
        }

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private List<String> buildParams(JmmNode node){
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        List<String> params = new ArrayList<>();
        for (var i = 1; i < node.getNumChildren(); i++){
            var child = node.getJmmChild(i);
            var childResult = visit(child);
            code.append(", ").append(childResult.getCode());
            computation.append(childResult.getComputation());
        }
        params.add(computation.toString());
        params.add(code.toString());

        return params;
    }

    private OllirExprResult visitMemberCallExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        var methodNode = node.getAncestor(METHOD_DECL).orElseThrow();
        var parent = node.getParent();
        boolean isReturnStmt = methodNode.getJmmChild(methodNode.getNumChildren() - 1).equals(node);

        while(!EXPR_STMT.check(parent) && !ASSIGN_STMT.check(parent) && !RETURN_STMT.check(parent) && !isReturnStmt && !MEMBER_CALL_EXPR.check(parent)){
            parent = parent.getParent();
        }
        var lhsName = node.getJmmChild(0).get("name");
        var lhsVisit = visit(node.getJmmChild(0));
        computation.append(lhsVisit.getComputation());
        var lhsCode = lhsVisit.getCode();

        String occurs = this.getClosestOccurrenceVariable(lhsName, methodNode.get("name"));
        var statOrVir = occurs.equals("import") ? "invokestatic(" : "invokevirtual(";

        var params = buildParams(node);

        if (EXPR_STMT.check(parent) ){
            String type = occurs.equals("import") || occurs.equals("local") || occurs.equals("param") || occurs.equals("method") ? "" : lhsName.equals("this") ? "" :
                    OptUtils.toOllirType(new Type(node.get("type"), false));
            String endType = OptUtils.toOllirType(new Type("void", false));
            String tmp = "";

            if (occurs.equals("field")){
                tmp = OptUtils.getTemp() + type;
                computation.append(tmp).append(SPACE).append(ASSIGN).append(type).append(SPACE)
                        .append("getfield(this, ").append(lhsCode).append(")").append(type).append(END_STMT);
            }
            computation.append(params.get(0));
            tmp = tmp.isEmpty() ? lhsCode + type : tmp;
            computation.append(statOrVir).append(tmp).append(", \"").append(node.get("name")).append("\"")
                    .append(params.get(1)).append(")").append(endType).append(END_STMT);

        }
        else {
            var type = ASSIGN_STMT.check(parent) ?
                    OptUtils.toOllirType(new Type(parent.getJmmChild(0).get("type"), false)) :
                    OptUtils.toOllirType(table.getReturnType(methodNode.get("name")));

            if (MEMBER_CALL_EXPR.check(parent)){
               // type = OptUtils.toOllirType(new Type(parent.get("type"), false));
                var child = node.getJmmChild(0);
                while (MEMBER_CALL_EXPR.check(child)){
                    child = child.getJmmChild(0);
                }
                var childName = child.get("name");
                type = childName.equals("this") ?
                        OptUtils.toOllirType(table.getReturnType(node.get("name"))) :
                        OptUtils.toOllirType(new Type(childName, false));
            }

            var tmp = OptUtils.getTemp() + type;
            String tmp2 = "";

            if (occurs.equals("field")){
                computation.append(tmp).append(SPACE).append(ASSIGN).append(type).append(SPACE)
                        .append("getfield(this, ").append(lhsCode).append(")").append(type).append(END_STMT);
                tmp2 = OptUtils.getTemp() + type;
            }
            computation.append(params.get(0));

            if (tmp2.isEmpty()){
                tmp2 = tmp;
                tmp = lhsCode;
            }
            computation.append(tmp2).append(SPACE).append(ASSIGN).append(type).append(SPACE)
                    .append(statOrVir).append(tmp).append(", \"").append(node.get("name"))
                    .append("\"").append(params.get(1)).append(")").append(type).append(END_STMT);

            code.append(tmp2);
        }



        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    //code is pretty much the tmp variable name
    //while computation is the code to compute the value to put in the tmp variable
    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        computation.append(node.get("op")).append(resOllirType).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        var classMethodParent = node;
        var parent = node.getParent();

        while (!METHOD_DECL.check(classMethodParent)){
            classMethodParent = classMethodParent.getParent();
        }
        var occurs = this.getClosestOccurrenceVariable(node.get("name"), classMethodParent.get("name"));


        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        String code = ollirType.equals(".") ? id :  id + ollirType;

        if (occurs.equals("field") ){
            if (parent.getJmmChild(1 % parent.getNumChildren()).equals(node)){
                var tmp = OptUtils.getTemp() + ollirType;
                computation.append(tmp).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE)
                        .append("getfield(this, ").append(code).append(")").append(ollirType).append(END_STMT);
                code = tmp;
            }
        }

        return new OllirExprResult(code, computation.toString());
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        for (var child : node.getChildren()) {
            OllirExprResult vis = visit(child);
            code.append(vis.getCode());
            computation.append(vis.getComputation());
        }

        return new OllirExprResult(code.toString(), computation.toString());
    }

}
