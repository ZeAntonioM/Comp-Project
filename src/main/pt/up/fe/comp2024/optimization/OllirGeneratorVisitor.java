package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";

    private static final String COMMA = ",";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(IMPORT_DECL, this::visitImport);
        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL_RULE, this::visitClass);
        addVisit(VAR_DECL, this::visitVarDecl);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM_DECL, this::visitParam);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(EXPR_STMT, this::visitExprStmt);
        addVisit(RETURN_STMT, this::visitReturn);

        setDefaultVisit(this::defaultVisit);
    }

    private String visitExprStmt(JmmNode jmmNode, Void unused) {
        var child = exprVisitor.visit(jmmNode.getJmmChild(0));
        return child.getComputation() + child.getCode() + NL;
    }

    private String visitImport(JmmNode jmmNode, Void unused) {
        var imports = jmmNode.getObjectAsList("name", String.class);
        if(jmmNode.get("isSubImport").equals("true")){
            return "import " + String.join(".", imports) + END_STMT;
        }
        else {
            return "import " + imports.get(0) + END_STMT;
        }

    }

    //? seems to be done?
    private String visitAssignStmt(JmmNode node, Void unused) {

        var lhs = exprVisitor.visit(node.getJmmChild(0));
        var rhs = exprVisitor.visit(node.getJmmChild(1));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(lhs.getComputation());
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of lhs
        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);


        code.append(lhs.getCode());
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();
    }

    private String visitReturn(JmmNode node, Void unused) {

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }

        code.append(expr.getComputation());
        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }

    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        return id + typeCode;
    }

    //TODO: WILL HAVE TYPE ANNOTATION
    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");
        boolean isStatic = NodeUtils.getBooleanAttribute(node, "isStatic", "false");
        boolean isMain = node.getNumChildren() == 0;

        if (isPublic) {
            code.append("public ");
        }

        if (isStatic) {
            code.append("static ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        code.append("(");
        // param
        var afterParam = 1;
        if (!isMain) {
            if (PARAM_DECL.check(node.getJmmChild(1))) {
                for (int i = 1; i < node.getNumChildren(); i++) {
                    var child = node.getJmmChild(i); //param
                    var paramCode = visit(child);
                    code.append(paramCode);
                    afterParam++;
                    if (i == node.getNumChildren() - 1) break;
                    code.append(COMMA);
                    code.append(SPACE);
                }
                code.delete(code.length() - 2, code.length() );
            }
        }

        code.append(")");

        // type
        var retType = OptUtils.toOllirType(new Type(node.get("type"), false));
        code.append(retType);
        code.append(L_BRACKET);
        //code.append(NL);

        // rest of its children stmts
        for (int i = afterParam; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            var childCode = visit(child);
            code.append(childCode);
        }

        //return
        //check has return
        if (!retType.equals(".V")) {
                var expr = exprVisitor.visit(node.getJmmChild(node.getNumChildren() - 1));
                code.append(expr.getComputation());
                code.append("ret");
                code.append(retType);
                code.append(SPACE);
                code.append(expr.getCode());
                code.append(END_STMT);
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        boolean hasSuperClass = NodeUtils.getBooleanAttribute(node, "hasSuperClass", "false");


        code.append(table.getClassName());
        if (hasSuperClass) {
            code.append(" extends ");
            code.append(node.get("superclass"));
        }
        code.append(L_BRACKET);

        code.append(NL);
        var needNl = true;

        for (var child : node.getChildren()) {
            var result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }

    private String visitVarDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        // fields and vars

        var type = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        if (CLASS_DECL_RULE.check(node.getParent())) {
            code.append(".field public ");
            code.append(id);
            code.append(type);
            code.append(END_STMT);
            code.append(NL);
        }
        return code.toString();
    }

    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
