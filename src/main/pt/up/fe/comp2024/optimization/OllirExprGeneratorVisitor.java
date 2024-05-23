package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;

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
        addVisit(BOOL_EXPR, this::visitBoolExpr);
        addVisit(NEG_EXPR, this::visitNegExpr);
        addVisit(SELF_EXPR, this::visitSelfExpr);
        addVisit(NEW_OBJ_EXPR, this::visitNewObjExpr);
        addVisit(ARRAY_REF_EXPR, this::visitArrayRefExpr);
        addVisit(ARRAY_INIT_EXPR, this::visitArrayInitExpr);
        addVisit(NEW_ARRAY_EXPR, this::visitNewArrayExpr);
        addVisit(LENGTH_EXPR, this::visitLengthExpr);

        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitLengthExpr(JmmNode jmmNode, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        var array = visit(jmmNode.getJmmChild(0));
        var tmp = OptUtils.getTemp() + ".i32";

        computation.append(array.getComputation());

        computation.append(tmp).append(SPACE).append(ASSIGN).append(".i32").append(SPACE)
                .append("arraylength(").append(array.getCode()).append(").i32").append(END_STMT);

        code.append(tmp);

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitNewArrayExpr(JmmNode jmmNode, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

       var size = visit(jmmNode.getJmmChild(0));

       computation.append(size.getComputation());

       code.append("new(array,").append(size.getCode()).append(").array.i32");

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitArrayInitExpr(JmmNode jmmNode, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();

        var tmp = OptUtils.getTemp();

        computation.append(tmp).append(".array.i32").append(SPACE).append(ASSIGN).append(".array.i32").append(SPACE).append("new(array,").append(SPACE)
                .append(jmmNode.getNumChildren()).append(".i32).array.i32").append(END_STMT);

        for (int i = 0; i < jmmNode.getNumChildren(); i++){
            var child = jmmNode.getJmmChild(i);
            var childResult = visit(child);
            computation.append(childResult.getComputation());
            computation.append(tmp).append("[").append(i).append(".i32].i32").append(SPACE).append(ASSIGN).append(".i32").append(SPACE).append(childResult.getCode()).append(END_STMT);
        }

        code.append(tmp).append(".array.i32");

        return new OllirExprResult(code.toString(), computation.toString());
        
    }

    private OllirExprResult visitArrayRefExpr(JmmNode jmmNode, Void unused) {
        var lhs = visit(jmmNode.getJmmChild(0));
        var index = visit(jmmNode.getJmmChild(1));

        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();
        computation.append(lhs.getComputation());
        computation.append(index.getComputation());

        String ollirType = ".i32";
        String tmp = OptUtils.getTemp() + ollirType;

        if (MEMBER_CALL_EXPR.check(jmmNode.getParent()) || ARRAY_REF_EXPR.check(jmmNode.getParent())) {
            computation.append(tmp).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE)
                    .append(lhs.getCode()).append("[").append(index.getCode()).append("]").append(ollirType).append(END_STMT);
            code.append(tmp);
        } else {
            code.append(lhs.getCode()).append("[").append(index.getCode()).append("]").append(ollirType);
        }

        return new OllirExprResult(code.toString(), computation.toString());
    }

    public String getClosestOccurrenceVariable(String variableName, String methodSignature) {
        if (variableName.equals("this")){
            return "class";
        }

        if (variableName.startsWith("tmp")){
            return "tmp";
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

    private List<String> buildParams(JmmNode node){
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        List<String> params = new ArrayList<>();

        var l = table.getParameters(node.get("name"));
        boolean checkVararg = false;
        String varargTmp = "";

        for (var i = 1; i < node.getNumChildren(); i++){
            if (!checkVararg && !l.isEmpty() && l.get((i - 1) % l.size()).getType().getName().equals("vararg")) {
                checkVararg = true;
                varargTmp = OptUtils.getTemp();
                computation.append(varargTmp).append(".array.i32").append(SPACE).append(ASSIGN).append(".array.i32").append(SPACE).append("new(array,").append(SPACE)
                        .append(node.getNumChildren() - i).append(".i32).array.i32").append(END_STMT);
            }
            if (checkVararg) {
                var child = visit(node.getJmmChild(i));
                computation.append(child.getComputation());
                computation.append(varargTmp).append("[").append(i - 1).append(".i32].i32").append(SPACE).append(ASSIGN).append(".i32").append(SPACE).append(child.getCode()).append(END_STMT);

                if (i == node.getNumChildren() - 1){
                    code.append(", ").append(varargTmp);
                }
            }
            else {
                var child = visit(node.getJmmChild(i));
                computation.append(child.getComputation());
                code.append(", ").append(child.getCode());
            }
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
            String type = occurs.equals("import") || occurs.equals("local") || occurs.equals("param") ? "" : lhsName.equals("this") ? "" :
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
                type = OptUtils.toOllirType(new Type(node.get("type"), false));
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

        if (node.get("op").equals("&&")) {

            computation.append(lhs.getComputation());

            var l1 = OptUtils.getTemp("L");
            var end = OptUtils.getTemp("END");
            computation.append("if (").append(lhs.getCode()).append(") goto ").append(l1).append(END_STMT);
            computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append("0.bool").append(END_STMT);
            computation.append("goto ").append(end).append(END_STMT);

            computation.append(l1).append(":\n");
            computation.append(rhs.getComputation());
            computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append(rhs.getCode()).append(END_STMT);

            computation.append(end).append(":\n");

        } else {
            computation.append(lhs.getComputation());
            computation.append(rhs.getComputation());

            computation.append(code).append(SPACE)
                    .append(ASSIGN).append(resOllirType).append(SPACE)
                    .append(lhs.getCode()).append(SPACE);

            computation.append(node.get("op")).append(resOllirType).append(SPACE)
                    .append(rhs.getCode()).append(END_STMT);
        }

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
        code = ARRAY_REF_EXPR.check(parent) && ollirType.equals(".array.i32") ? id : code;

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
