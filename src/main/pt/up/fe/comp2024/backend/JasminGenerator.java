package pt.up.fe.comp2024.backend;

import com.sun.jdi.ObjectReference;
import org.specs.comp.ollir.*;
import org.specs.comp.ollir.parser.OllirParser;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.ollir.OllirUtils;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2024.optimization.OllirGeneratorVisitor;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    private final FunctionClassMap<TreeNode, String> generators;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Field.class, this::generateField);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(CallInstruction.class, this::generateCallInstruction);
        generators.put(PutFieldInstruction.class, this::generatePutFieldInstruction);
        generators.put(GetFieldInstruction.class, this::generateGetFieldInstruction);
        generators.put(ReturnInstruction.class, this::generateReturn);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {

        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }

        return code;
    }


    private String generateClassUnit(ClassUnit classUnit) {

        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class ").append(className).append(NL);

        if (ollirResult.getOllirClass().getSuperClass() == null) {
            code.append(".super java/lang/Object").append(NL);
        }
        else {
            code.append(".super ").append(ollirResult.getOllirClass().getSuperClass()).append(NL);
        }

        // generate code for fields
        for (var field : ollirResult.getOllirClass().getFields()) {
            code.append(generators.apply(field));
        }

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {
            code.append(generators.apply(method));
        }

        return code.toString();
    }

    private String generateField(Field field) {
        var code = new StringBuilder();

        var modifier = field.getFieldAccessModifier() != AccessModifier.DEFAULT ?
                field.getFieldAccessModifier().name().toLowerCase() + " " :
                "";

        var fieldName = field.getFieldName();
        var fieldType = this.getType(field.getFieldType().getTypeOfElement());

        code.append(".field ").append(modifier).append(fieldName).append(" ").append(fieldType).append(NL);

        return code.toString();
    }

    private String generateMethod(Method method) {

        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var isStatic = method.isStaticMethod() ? "static " : "";
        modifier += isStatic;

        var methodName = method.getMethodName();

        var returnType = this.getType(method.getReturnType().getTypeOfElement());

        code.append("\n.method ").append(modifier);
        code.append((method.isConstructMethod() ? "<init>" : methodName));
        code.append("(");

        for (var param: method.getParams()) {
            code.append(getType(param.getType().getTypeOfElement()));
        }

        code.append(")").append(returnType).append(NL);

        // Add limits
        code.append(TAB).append(".limit stack 99").append(NL);
        code.append(TAB).append(".limit locals 99").append(NL);

        for (var inst : method.getInstructions()) {
            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);

            if (inst instanceof CallInstruction && ((CallInstruction) inst).getReturnType().getTypeOfElement() != ElementType.VOID) {
                code.append(TAB).append("pop").append(NL);
            }

        }

        //check return instruction
        if (method.getInstructions().isEmpty() ||
                (
                        !(method.getInstructions().get(method.getInstructions().size() - 1) instanceof ReturnInstruction)
                                &&
                                (method.getReturnType().getTypeOfElement() == ElementType.VOID))
        )
        {
            code.append(TAB).append("return").append(NL);
        }



        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // generate code for loading what's on the right
        code.append(generators.apply(assign.getRhs()));

        // store value in the stack in destination
        var lhs = assign.getDest();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        var type = assign.getTypeOfAssign().getTypeOfElement();

        switch (type) {
            case INT32, BOOLEAN -> code.append("istore ").append(reg).append(NL);
            case ARRAYREF, OBJECTREF -> code.append("astore ").append(reg).append(NL);
        }

        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        return "ldc " + literal.getLiteral() + NL;
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        return switch (operand.getType().getTypeOfElement()) {
            case INT32, BOOLEAN -> "iload " + reg + NL;
            case STRING, ARRAYREF, OBJECTREF -> "aload " + reg + NL;
            default -> null;
        };

    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd ";
            case MUL -> "imul ";
            case SUB -> "isub ";
            case DIV -> "idiv ";
            case AND -> "iand ";
            case GTH -> "if_icmpgt ";
            case LTH -> "if_icmplt ";
            default -> null;
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateCallInstruction(CallInstruction callInst) {
        var code = new StringBuilder();

        var callType = callInst.getInvocationType();

        switch (callType) {

            case invokespecial:
                code.append(invokeSpecial(callInst));
                break;

            case invokestatic:
                code.append(invokeStatic(callInst));
                break;

            case invokevirtual:
                code.append(invokeVirtual(callInst));
                break;

            case arraylength:
                code.append("arraylength").append(NL);
                break;

            case NEW:

                var ret = new StringBuilder();

                var className = ((Operand) callInst.getCaller()).getName();

                ret.append("new ").append(className).append(NL);

                code.append(ret);

                break;

        }

        return code.toString();
    }



    private String invokeSpecial(CallInstruction callInst) {
        var code = new StringBuilder();

        var operands = callInst.getOperands();

        switch(operands.get(0).getType().getTypeOfElement()) {

            case THIS:
                code.append("aload_0").append(NL);
                break;
            case OBJECTREF, ARRAYREF:
                code.append("aload ").append(currentMethod.getVarTable().get(((Operand) operands.get(0)).getName()).getVirtualReg()).append(NL);
                break;
            case INT32, BOOLEAN:
                code.append("iload ").append(currentMethod.getVarTable().get(((Operand) operands.get(0)).getName()).getVirtualReg()).append(NL);
                break;
        }

        code.append("invokespecial ");


        if (operands.get(0).getType().getTypeOfElement() == ElementType.THIS) {

            if (currentMethod.getOllirClass().getSuperClass() == null) {
                code.append("java/lang/Object");
            }
            else {
                var className = currentMethod.getOllirClass().getSuperClass();
                code.append(className);
            }

            code.append("/<init>(");

            for (int i = 2; i < operands.size(); i++) {
                code.append(getType(operands.get(i).getType().getTypeOfElement()));
            }

            code.append(")");

        }
        else {

            var classType = operands.get(0).getType();
            var elemTypeLen = (classType.getTypeOfElement().toString()).length();
            var className = classType.toString().substring(elemTypeLen+1, classType.toString().length() - 1);

            code.append(className).append("/").append("<init>");
            code.append("(");

            for (int i = 2; i < operands.size(); i++) {

                code.append(operands.size()).append(NL);
                code.append(getType(operands.get(i).getType().getTypeOfElement()));
            }

            code.append(")");
        }

        code.append(getType(callInst.getReturnType().getTypeOfElement())).append(NL);

        return code.toString();
    }

    private String invokeStatic(CallInstruction callInst) {
        var code = new StringBuilder();

        var args = callInst.getArguments();
        var operands = callInst.getOperands();

        // load arguments
        for (var arg : args) {
            code.append(generators.apply(arg));
        }

        var className = ((Operand) callInst.getCaller()).getName();

        var method = callInst.getMethodName();
        var literal = ((LiteralElement) method).getLiteral();
        if (literal.startsWith("\"") && literal.endsWith("\"") && literal.length() > 1) {
            literal = literal.substring(1, literal.length() - 1);
        }

        code.append("invokestatic ").append(className).append("/").append(literal).append("(");

        for (int i = 2; i < operands.size(); i++) {
            code.append(getType(operands.get(i).getType().getTypeOfElement()));
        }

        code.append(")").append(getType(callInst.getReturnType().getTypeOfElement())).append(NL);

        return code.toString();
    }

    private String invokeVirtual(CallInstruction callInst) {
        var code = new StringBuilder();

        var operands = callInst.getOperands();

        // load object
        if (operands.get(0).getType().getTypeOfElement() == ElementType.THIS) {
            code.append("aload_0").append(NL);
        }
        else {
            code.append("aload ").append(currentMethod.getVarTable().get(((Operand) operands.get(0)).getName()).getVirtualReg()).append(NL);
        }

        // load arguments
        for (int i = 2; i < operands.size(); i++) {
            code.append(generators.apply(operands.get(i)));
        }

        var className = ollirResult.getSymbolTable().getClassName();

        var method = callInst.getMethodName();
        var literal = ((LiteralElement) method).getLiteral();
        if (literal.startsWith("\"") && literal.endsWith("\"") && literal.length() > 1) {
            literal = literal.substring(1, literal.length() - 1);
        }

        code.append("invokevirtual ").append(className).append("/").append(literal).append("(");

        for (int i = 2; i < callInst.getOperands().size(); i++) {
            code.append(getType(callInst.getOperands().get(i).getType().getTypeOfElement()));
        }

        code.append(")").append(getType(callInst.getReturnType().getTypeOfElement())).append(NL);

        return code.toString();
    }

    private String generatePutFieldInstruction(PutFieldInstruction putFieldInst) {
        var code = new StringBuilder();

        var field = putFieldInst.getField().getName();
        var object = putFieldInst.getObject();

        var value = putFieldInst.getValue();

        code.append("aload ").append(currentMethod.getVarTable().get(object.getName()).getVirtualReg()).append(NL);

        code.append(generators.apply(value));

        var className = currentMethod.getOllirClass().getClassName();

        code.append("putfield ").append(className).append("/").append(field).append(" ");

        code.append(getType(putFieldInst.getValue().getType().getTypeOfElement())).append(NL);

        return code.toString();
    }

    private String generateGetFieldInstruction(GetFieldInstruction getFieldInst) {
        var code = new StringBuilder();

        var field = getFieldInst.getField().getName();
        var object = getFieldInst.getObject();

        code.append("aload ").append(currentMethod.getVarTable().get(object.getName()).getVirtualReg()).append(NL);

        var className = currentMethod.getOllirClass().getClassName();

        code.append("getfield ").append(className).append("/").append(field).append(" ");

        code.append(getType(getFieldInst.getField().getType().getTypeOfElement())).append(NL);


        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        var type = returnInst.getReturnType().getTypeOfElement();

        if (returnInst.getOperand() != null)
            code.append(generators.apply(returnInst.getOperand()));

        switch (type) {
            case INT32, BOOLEAN -> code.append("ireturn").append(NL);
            case ARRAYREF, OBJECTREF -> code.append("areturn").append(NL);
            case VOID -> code.append("return").append(NL);
        }

        return code.toString();
    }

    private String getType(ElementType type) {
        return switch (type) {
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            case VOID -> "V";
            case STRING -> "Ljava/lang/String;";
            case ARRAYREF -> "[Ljava/lang/String;";
            case OBJECTREF -> "L";
            default -> null;
        };
    }


}
