package pt.up.fe.comp2024.optimization;

import org.junit.Test;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.function.BiFunction;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

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

        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitSelfExpr(JmmNode node, Void unused) {
        return new OllirExprResult("this");
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

    private OllirExprResult visitMemberCallExpr(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        var parent = node.getParent();
        String type = OptUtils.toOllirType(new Type("void", false));
        if (ASSIGN_STMT.check(parent)) {
            type = OptUtils.toOllirType(TypeUtils.getExprType(parent.getJmmChild(0), table));
        }

        var lhs = visit(node.getJmmChild(0));
        var lhs_code = lhs.getCode();

        //TODO: make this work when I have type annotation
        code.append("invokestatic(").append(lhs_code).append(", \"").append(node.get("name")).append("\"");


        for (int i = 1; i < node.getNumChildren(); i++) {
            code.append(", ");
            code.append(visit(node.getJmmChild(i)).getCode());
        }

        code.append(")").append(type).append(";");

        return new OllirExprResult(code.toString());
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

        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        String code = ollirType.equals(".") ? id :  id + ollirType;

        return new OllirExprResult(code);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
