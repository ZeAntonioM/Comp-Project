package pt.up.fe.comp2024.backend;

import com.sun.jdi.ObjectReference;
import org.specs.comp.ollir.*;
import org.specs.comp.ollir.parser.OllirParser;
import org.specs.comp.ollir.tree.TreeNode;
import org.w3c.dom.css.CSSImportRule;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.ollir.OllirUtils;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp2024.optimization.OllirGeneratorVisitor;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.awt.*;
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

    int maxStack = 0;
    int currentStack = 0;
    int locals = 0;

    int branchCounter = 0;

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
        generators.put(UnaryOpInstruction.class, this::generateUnaryOp);
        generators.put(CallInstruction.class, this::generateCallInstruction);
        generators.put(PutFieldInstruction.class, this::generatePutFieldInstruction);
        generators.put(GetFieldInstruction.class, this::generateGetFieldInstruction);
        generators.put(CondBranchInstruction.class, this::generateCondBranch);
        generators.put(GotoInstruction.class, this::generateGoToInstruction);
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
        var className = importCorrection(ollirResult.getOllirClass().getClassName());
        code.append(".class ").append(className).append(NL);

        if (ollirResult.getOllirClass().getSuperClass() == null) {
            code.append(".super java/lang/Object").append(NL);
        }
        else {
            var superClassName = getImportedClass(ollirResult.getOllirClass().getSuperClass());
            code.append(".super ").append(superClassName).append(NL);
        }

        // generate code for fields
        for (var field : ollirResult.getOllirClass().getFields()) {
            code.append(generators.apply(field));
        }

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {
            code.append(generators.apply(method));
        }


        System.out.println(code);
        return code.toString();
    }

    private String generateField(Field field) {
        var code = new StringBuilder();

        var modifier = getFieldModifier(field);

        var fieldName = field.getFieldName();
        var fieldType = this.getType(field.getFieldType());

        code.append(".field ").append(modifier).append(fieldName).append(" ").append(fieldType).append(NL);

        return code.toString();
    }

    private String generateMethod(Method method) {

        // set method
        currentMethod = method;
        locals = 0;
        maxStack = 0;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "public ";

        modifier += method.isFinalMethod() ? "final " : "";
        modifier += method.isStaticMethod() ? "static " : "";


        var methodName = method.getMethodName();

        code.append("\n.method ").append(modifier);
        code.append((method.isConstructMethod() ? "<init>" : methodName));
        code.append("(");

        for (var param: method.getParams()) {
            code.append(getType(param.getType()));
        }
        updateLocals(method.getParams().size());

        var returnType = this.getType(method.getReturnType());

        code.append(")").append(returnType).append(NL);

        var methodBody = new StringBuilder();
        for (var inst : method.getInstructions()) {

            for (var label : method.getLabels().entrySet()) {
                if (label.getValue().equals(inst)) {
                    methodBody.append(label.getKey()).append(":").append(NL);
                }
            }

            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            methodBody.append(instCode);



            if ((inst.getInstType() == InstructionType.CALL) &&  ((CallInstruction) inst).getReturnType().getTypeOfElement() != ElementType.VOID) {
                methodBody.append("pop").append(NL);
                this.updateStack(-1);
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
            methodBody.append(TAB).append("return").append(NL);
        }

        int l = method.getVarTable().size();
        if (!method.getVarTable().containsKey("this")) l++;

        // Add limits
        code.append(TAB).append(".limit stack ").append(maxStack).append(NL);
        //code.append(TAB).append(".limit locals ").append(locals+1).append(NL);
        code.append(TAB).append(".limit locals ").append(l).append(NL);
        code.append(methodBody);
        code.append(".end method\n");

        // unset method
        currentMethod = null;

        return code.toString();
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        // store value in the stack in destination
        var lhs = assign.getDest();

        // Array assignment
        if (lhs instanceof ArrayOperand) {
            return generateArrayAssign(assign);
        }

        // Use of iinc
        if (assign.getRhs() instanceof BinaryOpInstruction) {
            var rhs = (BinaryOpInstruction) assign.getRhs();
            var operation = rhs.getOperation().getOpType();

            // Can only use iinc if the operation is add or sub
            if (operation == OperationType.ADD || operation == OperationType.SUB) {

                var reg = 0;
                var literal = "";

                // we need to check the two different cases: lhs = lhs + literal and lhs = literal + lhs
                if (rhs.getLeftOperand() instanceof Operand) {
                    var leftOperand = (Operand) rhs.getLeftOperand();

                    // Check if name is the same as the operand on assign: lhs = lhs + literal
                    if (leftOperand.getName().equals(((Operand) lhs).getName())) {
                        reg = currentMethod.getVarTable().get(leftOperand.getName()).getVirtualReg();
                        updateLocals(reg);

                        // We need to check if the right operand is a literal, otherwise we can't use iinc
                        if (rhs.getRightOperand() instanceof LiteralElement) {
                            literal = ((LiteralElement) rhs.getRightOperand()).getLiteral();

                        }
                    }
                }

                if (rhs.getRightOperand() instanceof Operand) {
                    var rightOperand = (Operand) rhs.getRightOperand();

                    // Check if name is the same as the operand on assign: lhs = literal + lhs
                    if (rightOperand.getName().equals(((Operand) lhs).getName())) {
                        reg = currentMethod.getVarTable().get(rightOperand.getName()).getVirtualReg();
                        updateLocals(reg);

                        // We need to check if the left operand is a literal, otherwise we can't use iinc
                        if (rhs.getLeftOperand() instanceof LiteralElement) {
                            literal = ((LiteralElement) rhs.getLeftOperand()).getLiteral();
                        }
                    }
                }

                // If we have a register and a literal, we can use iinc
                if (reg != 0 && !literal.isEmpty()) {
                    var isNeg = operation == OperationType.SUB ? "-" : "";
                    code.append("iinc ").append(reg).append(" ").append(isNeg).append(literal).append(NL);
                    return code.toString();
                }
            }
        }

        // If not array assignment, nor iinc, we need to generate the code for the right side
        code.append(generators.apply(assign.getRhs()));

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        updateLocals(reg);

        var str = reg < 4 ? "_" + reg : " " + reg + NL;
        var type = assign.getTypeOfAssign().getTypeOfElement();
        switch (type) {
            case INT32, BOOLEAN -> code.append("istore").append(str);
            case STRING, ARRAYREF, OBJECTREF -> code.append("astore").append(str);
            default -> throw new NotImplementedException(operand.getType().getTypeOfElement());
        };

        updateStack(-1); // pop value
        return code.toString();
    }

    private String generateArrayAssign(AssignInstruction assign) {

        // The correct order for iastore is arrayref, index and value, so we need to load the arrayref first
        var code = new StringBuilder();

        var operand = (Operand) assign.getDest();

        // arrayRef
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        updateLocals(reg);
        code.append("aload").append(reg < 4 ? "_" + reg : " " + reg).append(NL);
        updateStack(1); // push array reference

        //get index
        var array = (ArrayOperand) assign.getDest();
        code.append(generators.apply( array.getIndexOperands().get(0) ));

        //get value
        code.append(generators.apply(assign.getRhs()));

        code.append("iastore").append(NL);
        updateStack(-3); // pop array reference, index and value


        return code.toString();

    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {

        ElementType type = literal.getType().getTypeOfElement();

        // update stack since we are pushing a literal
        updateStack(1);

        // ldc also works for strings, floats, etc...
        if ((type != ElementType.INT32) && (type != ElementType.BOOLEAN)) {
            return "ldc " + literal.getLiteral() + NL;
        }

        var value = Integer.parseInt(literal.getLiteral());
        if ( (value <= 5) && (value >= -1)) {
            if (value == -1) {
                return "iconst_m1" + NL;
            }
            return "iconst_" + value + NL;
        }
        if ( (value <= 127) && (value >= -128)) {
            return "bipush " + value + NL;
        }
        if ( (value <= 32767) && (value >= -32768)) {
            return "sipush " + value + NL;
        }
        else {
            return "ldc " + value + NL;
        }

    }

    private String generateOperand(Operand operand) {

        if (operand instanceof ArrayOperand) {
            return generateArrayOperand(operand);
        }

        var code = new StringBuilder();

        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        updateLocals(reg);
        updateStack(1); // push value to stack

        String str = reg < 4 ? "_" + reg : " " + reg;
        switch(operand.getType().getTypeOfElement()) {
            case THIS:
                code.append("aload_0").append(NL);
                break;
            case OBJECTREF, STRING,ARRAYREF:
                code.append("aload").append(str).append(NL);
                break;
            case INT32, BOOLEAN:
                code.append("iload").append(str).append(NL);
                break;

        }

        return code.toString();

    }

    private String generateArrayOperand(Operand operand) {

            var code = new StringBuilder();

            // get register
            var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
            updateLocals(reg);
            updateStack(1); // push arrayRef to stack
            String str = reg < 4 ? "_" + reg : " " + reg;
            code.append("aload").append(str).append(NL);
            updateStack(1);

            // get Index
            var array = (ArrayOperand) operand;
            code.append(generators.apply(array.getIndexOperands().get(0)));

            code.append("iaload").append(NL);
            updateStack(-2); // pop array reference and index
            updateStack(1); // push value

            return code.toString();
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = new StringBuilder();
        switch (binaryOp.getOperation().getOpType()) {
            case ADD:
                op.append("iadd ");
                updateStack(-2); // pop two values
                updateStack(1); // push result
                break;
            case MUL:
                op.append("imul ");
                updateStack(-2); // pop two values
                updateStack(1); // push result
                break;
            case SUB:
                op.append("isub ");
                updateStack(-2); // pop two values
                updateStack(1); // push result
                break;
            case DIV:
                op.append("idiv ");
                updateStack(-2); // pop two values
                updateStack(1); // push result
                break;
            case AND:
                op.append("iand ");
                updateStack(-2); // pop two values
                updateStack(1); // push result
                break;
            case LTH:
                op.append("isub ").append(NL);
                updateStack(-2); // pop two values
                updateStack(1); // push result

                op.append("iflt ");
                updateStack(-1); // pop boolean value

                op.append(this.boolBranching());
                break;
            case GTH:
                op.append("isub ").append(NL);
                updateStack(-2); // pop two values
                updateStack(1); // push result

                op.append("ifgt ");
                updateStack(-1); // pop boolean value

                op.append(this.boolBranching());
                break;
            case EQ:
                op.append("isub ").append(NL);
                updateStack(-2); // pop two values
                updateStack(1); // push result

                op.append("ifeq ");
                updateStack(-1); // pop boolean value

                op.append(this.boolBranching());
                break;
            case NEQ:
                op.append("isub ").append(NL);
                updateStack(-2); // pop two values
                updateStack(1); // push result

                op.append("ifne ");
                updateStack(-1); // pop boolean value

                op.append(this.boolBranching());
                break;

            default:
                break;
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateUnaryOp(UnaryOpInstruction unaryOp) {
        var code = new StringBuilder();

        // load value
        code.append(generators.apply(unaryOp.getOperand()));

        // apply operation
        var op = new StringBuilder();
        System.out.println("UNARY: " + unaryOp);
        switch (unaryOp.getOperation().getOpType()) {
            // XOR -> 1 if 1 operand is true, 0 if both are true or false
            case NOTB:
                op.append("iconst_1 ").append(NL).append("ixor ");
                break;
            default:
                break;
        };

        updateStack(1); //iconst_1 pushes a value
        updateStack(-2); //xor pops two values
        updateStack(1); // push result

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

                code.append(generators.apply(callInst.getCaller()));
                code.append("arraylength").append(NL);
                updateStack(-1); // pop array reference
                updateStack(1); // push length
                break;

            case NEW:
                code.append(generateNew(callInst));
                break;

        }

        return code.toString();
    }



    private String invokeSpecial(CallInstruction callInst) {
        var code = new StringBuilder();

        var args = callInst.getArguments();

        code.append(generators.apply((Operand) callInst.getCaller()));

        for (var arg : args) {
            code.append(generators.apply(arg));
        }

        code.append("invokespecial ");


        if (callInst.getCaller().getType().getTypeOfElement() == ElementType.THIS) {

            if (currentMethod.getOllirClass().getSuperClass() == null) {
                code.append("java/lang/Object");
            }
            else {
                var className = currentMethod.getOllirClass().getSuperClass();
                var superClass = getImportedClass(className);
                code.append(superClass);
            }

        }
        else {
            var className = getImportedClass(((ClassType) callInst.getCaller().getType()).getName());
            code.append(className);
        }

        // Need to check if the method is a constructor
        var method = callInst.getMethodName();
        if (method instanceof LiteralElement) {
            var literal = ((LiteralElement) method).getLiteral().replace("\"", "");
            code.append("/").append(literal);

            code.append("(");
            for (var arg: args) {
                code.append(getType(arg.getType()));
            }
            code.append(")");

        }
        else { code.append("/<init>()"); }

        code.append(getType(callInst.getReturnType())).append(NL);

        updateStack(-args.size());
        updateStack(-1); // pop caller
        if (callInst.getReturnType().getTypeOfElement() != ElementType.VOID) {
            updateStack(1); // push return value
        }

        return code.toString();
    }

    private String invokeStatic(CallInstruction callInst) {
        var code = new StringBuilder();

        var args = callInst.getArguments();

        // load arguments
        for (var arg : args) {
            code.append(generators.apply(arg));
        }

        var className = getImportedClass(((Operand) callInst.getCaller()).getName());

        var method = callInst.getMethodName();
        var literal = ((LiteralElement) method).getLiteral().replace("\"", "");

        code.append("invokestatic ").append(className).append("/").append(literal).append("(");

        for (var arg: args) {
            code.append(getType(arg.getType()));
        }

        code.append(")").append(getType(callInst.getReturnType())).append(NL);

        updateStack(-args.size()); // pop arguments
        if (callInst.getReturnType().getTypeOfElement() != ElementType.VOID) {
            updateStack(1); // push return value
        }

        return code.toString();
    }

    private String invokeVirtual(CallInstruction callInst) {
        var code = new StringBuilder();

        var args = callInst.getArguments();

        // load object
        code.append(generators.apply((Operand) callInst.getCaller()));

        // load arguments
        for (var arg : args) {
            code.append(generators.apply(arg));
        }

        var className = getImportedClass(((ClassType) callInst.getCaller().getType()).getName());

        var method = callInst.getMethodName();
        var literal = ((LiteralElement) method).getLiteral().replace("\"", "");

        code.append("invokevirtual ").append(className).append("/").append(literal).append("(");

        for (var arg: callInst.getArguments()) {
            code.append(getType(arg.getType()));
        }

        code.append(")").append(getType(callInst.getReturnType())).append(NL);

        updateStack(-1); // pop caller
        updateStack(-args.size()); // pop arguments
        if (callInst.getReturnType().getTypeOfElement() != ElementType.VOID) {
            updateStack(1); // push return value
        }

        return code.toString();
    }

    private String generateNew(CallInstruction callInst) {

        var code = new StringBuilder();

        var args = callInst.getArguments();

        for (var arg : args) {
            code.append(generators.apply(arg));
        }

        if (callInst.getCaller().getType().getTypeOfElement() == ElementType.ARRAYREF) {

            code.append("newarray int").append(NL);
            updateStack(-1); // pop size
            updateStack(1); // push array reference

        }
        else {
            var className = getImportedClass(((ClassType) callInst.getCaller().getType()).getName());
            code.append("new ").append(className).append(NL);
            updateStack(-args.size()); // pop arguments
            updateStack(1); // push object
        }

        return code.toString();
    }
    private String generatePutFieldInstruction(PutFieldInstruction putFieldInst) {
        var code = new StringBuilder();

        var field = putFieldInst.getField().getName();
        var object = putFieldInst.getObject();

        code.append(generators.apply(object));

        code.append(generators.apply(putFieldInst.getValue()));

        var className = getImportedClass(((ClassType) object.getType()).getName());

        code.append("putfield ").append(className).append("/").append(field).append(" ");
        updateStack(-2); // pop object reference and value

        code.append(getType(putFieldInst.getValue().getType())).append(NL);

        return code.toString();
    }

    private String generateGetFieldInstruction(GetFieldInstruction getFieldInst) {
        var code = new StringBuilder();

        var field = getFieldInst.getField().getName();
        var object = getFieldInst.getObject();

        code.append(generators.apply(object));

        var className = getImportedClass(((ClassType) object.getType()).getName());

        code.append("getfield ").append(className).append("/").append(field).append(" ");
        updateStack(-1); // pop object reference
        updateStack(1); // push value

        if (getFieldInst.getField().getType().getTypeOfElement() == ElementType.ARRAYREF) {
            code.append("[I").append(NL);
            return code.toString();
        }
        code.append(getType(getFieldInst.getField().getType())).append(NL);

        return code.toString();
    }

    private String generateCondBranch(CondBranchInstruction condBranch){

        var code = new StringBuilder();

        code.append(generators.apply(condBranch.getCondition()));

        //Check if the condition is true or false
        code.append("ifne ").append(condBranch.getLabel()).append(NL);
        updateStack(-1); // pops boolean value

        return code.toString();

    }

    private String generateGoToInstruction(GotoInstruction gotoInst) {
        return "goto " + gotoInst.getLabel() + NL;
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        var type = returnInst.getReturnType().getTypeOfElement();

        if (returnInst.getOperand() != null){
            code.append(generators.apply(returnInst.getOperand()));
        }

        switch (type) {
            case INT32, BOOLEAN:
                code.append("ireturn").append(NL);
                updateStack(-1);
                break;
            case ARRAYREF, OBJECTREF:
                code.append("areturn").append(NL);
                updateStack(-1);
                break;
            case VOID:
                code.append("return").append(NL);
                break;

        }

        return code.toString();
    }

    private String getImportedClass(String className) {

        if (className.equals("this")) {
            return ollirResult.getOllirClass().getClassName();
        }

        for (String imported : ollirResult.getOllirClass().getImports()) {
            if (imported.endsWith(className)) {
                return importCorrection(imported);
            }
        }

        return className;
    }

    private String importCorrection(String className) {
        return className.replace(".", "/");
    }

    private String getFieldModifier(Field field) {
        var modifier = field.getFieldAccessModifier() != AccessModifier.DEFAULT ?
                field.getFieldAccessModifier().name().toLowerCase() + " " :
                "public ";
        if (field.isFinalField()) {
            modifier += "final ";
        }
        if (field.isStaticField()) {
            modifier += "static ";
        }
        return modifier;
    }

    private String getType(Type type) {

        var code = "";

        switch (type.getTypeOfElement()) {
            case INT32 -> code = "I";
            case BOOLEAN -> code = "Z";
            case VOID -> code = "V";
            case STRING -> code = "Ljava/lang/String;";
            case ARRAYREF -> code = "[" + this.getType(((ArrayType) type).getElementType());
            case OBJECTREF -> code = "L"+getImportedClass(((ClassType) type).getName())+";";
            default -> code = null;
        }

        return code;
    }

    private void updateStack(int value) {
        currentStack += value;
        if (currentStack > maxStack) {
            maxStack = currentStack;
        }
    }

    private void updateLocals(int value) {
        locals = Math.max(value, locals);
    }

    private String boolBranching() {
        var code = new StringBuilder();

        // goto if correct
        code.append("branch_").append(branchCounter).append(NL);

        // if not equal
        code.append("iconst_0").append(NL);
        updateStack(1);
        code.append("goto ").append("end_branch_").append(branchCounter).append(NL).append(NL);

        // if equal
        code.append("branch_").append(branchCounter).append(":").append(NL);
        code.append("iconst_1").append(NL).append(NL);
        updateStack(1);

        // end branch
        code.append("end_branch_").append(branchCounter).append(":");

        branchCounter++;

        return code.toString();
    }


}
