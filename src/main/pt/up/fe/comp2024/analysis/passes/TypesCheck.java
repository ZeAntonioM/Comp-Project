package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.analysis.Utils;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Checks if the operands of an operation have types compatible with the operation
 *
 */
public class TypesCheck extends AnalysisVisitor {

    private final Map<String, String> classAndSuperClass = new HashMap<>();
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.IF_ELSE_STMT, this::visitCondition);
        addVisit(Kind.CLASS_DECL_RULE, this::visitClassDeclRule);
        addVisit(Kind.WHILE_STMT, this::visitCondition);
        addVisit(Kind.MEMBER_CALL_EXPR, this::visitMemberCallExpr);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }


    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        // Get the left and right operands
        var leftOperand = binaryExpr.getChildren().get(0);
        var rightOperand = binaryExpr.getChildren().get(1);

        // Initialize the types of the operands
        String leftType = Utils.getOperandType(leftOperand, table, currentMethod);
        String rightType = Utils.getOperandType(rightOperand, table, currentMethod);


        leftOperand.put("type", leftType);
        rightOperand.put("type", rightType);

        // Get the operator
        var operator = binaryExpr.get("op");

        var expectedType = switch (operator) {
            case "+", "-", "*", "/", "<", ">" -> "int";
            case "&&" -> "boolean";
            default -> throw new IllegalArgumentException("Unknown operator: " + operator);
        };

        binaryExpr.put("type", expectedType);

        if (!Objects.equals(leftType, expectedType) || !Objects.equals(rightType, expectedType)) {
            var message = String.format("Operator '%s' expects operands of type '%s', but got '%s' and '%s'", operator, expectedType, leftType, rightType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(binaryExpr),
                    NodeUtils.getColumn(binaryExpr),
                    message,
                    null
            ));
        }


        return null;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        var assigned = assignStmt.getChildren().get(0);
        var value = assignStmt.getChildren().get(1);

        if (value.getKind().equals(Kind.NEW_ARRAY_EXPR.toString())) {
            var size = value.getChildren().get(0);
            var assignedType = Utils.getOperandType(size, table, currentMethod);
            if (!assignedType.equals("int")) {
                var message = String.format("Cannot assign '%s' as the size of the array", size.get("bool"));
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null
                ));
            }
        } else {
            var assignedType = Utils.getOperandType(assigned, table, currentMethod);
            var valueType = Utils.getOperandType(value, table, currentMethod);
            var imports = table.getImports();

            // Check if assignedType and valueType are not null
            boolean typesNotNull = assignedType != null && valueType != null;

            // Check if valueType is not equal to className, superClassName, and assignedType
            boolean typesNotEqual = !(Objects.equals(valueType, assignedType)
                    || (classAndSuperClass.containsKey(valueType) && Objects.equals(classAndSuperClass.get(valueType), assignedType)));

            // Check the types are not equal to the imports
            boolean typesImported = imports.contains(assignedType) && imports.contains(valueType);

            // If both conditions are true, add an error report
            if (typesNotNull && typesNotEqual && !typesImported) {
                String message = String.format("Cannot assign a value of type '%s' to a variable of type '%s'", valueType, assignedType);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(assignStmt),
                        NodeUtils.getColumn(assignStmt),
                        message,
                        null
                ));
            }
        }



        return null;
    }

    private Void visitClassDeclRule(JmmNode classDeclRule, SymbolTable table) {
        var className = classDeclRule.get("name");
        var superClassName = classDeclRule.get("hasSuperClass").equals("true") ? classDeclRule.get("superclass") : null;
        classAndSuperClass.put(className, superClassName);
        return null;
    }

    private Void visitCondition(JmmNode conditionExpr, SymbolTable table) {
        var condition = conditionExpr.getChildren().get(0);
        String conditionType = Utils.getOperandType(condition, table, currentMethod);
        if (!Objects.equals(conditionType, "boolean")) {
            var message = String.format("Condition must be a boolean expression, but found '%s'", conditionType);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(conditionExpr),
                    NodeUtils.getColumn(conditionExpr),
                    message,
                    null
            ));
        }

        return null;
    }

    private Void visitMemberCallExpr(JmmNode memberCallExpr, SymbolTable table) {
        // Extract method name from the member call expression
        var methodName = memberCallExpr.get("id");

        // Get the first child of the member call expression, which should be the object on which the method is called
        var objectNode = memberCallExpr.getChildren().get(0);

        // Get the parameters of the method
        var parameters = memberCallExpr.getChildren().subList(1, memberCallExpr.getChildren().size());

        // Determine the type of the object
        String objectClassName;
        if (objectNode.getKind().equals(Kind.SELF_EXPR.toString())) {
            // If the object is 'this', the class name is the current class
            objectClassName = table.getClassName();
        } else {
            objectClassName = Utils.getOperandType(objectNode, table, currentMethod);
        }

        // Get the list of methods and imports from the symbol table
        var availableMethods = table.getMethods();
        var importedClasses = table.getImports();

        // Get the return type of the method
        var returnType = table.getReturnType(currentMethod).getName();

        // Check if the object's class or its superclass is imported
        var superClass = classAndSuperClass.get(objectClassName);
        var isSuperClassImported = importedClasses.contains(superClass);
        var isClassImported = importedClasses.contains(objectClassName);


        if (superClass == null && !isClassImported && table.getClassName().contains(objectClassName)) {
            if (!availableMethods.contains(methodName)) {
                var errorMessage = String.format("Class '%s' does not contain a method '%s'", objectClassName, methodName);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(memberCallExpr),
                        NodeUtils.getColumn(memberCallExpr),
                        errorMessage,
                        null
                ));
            }

            else {
                var declaredParameters = table.getParameters(methodName);
                boolean isVarargs = declaredParameters.stream()
                        .anyMatch(symbol -> symbol.getType().getName().equals("vararg"));


                if (isVarargs && !declaredParameters.isEmpty() && !declaredParameters.get(declaredParameters.size() - 1).getType().getName().equals("vararg")) {
                    var errorMessage = String.format("Method '%s' expects varargs to be the last parameter", methodName);
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(memberCallExpr),
                            NodeUtils.getColumn(memberCallExpr),
                            errorMessage,
                            null
                    ));
                }
                // Check if the number of parameters is the same
                else if ((!isVarargs && declaredParameters.size() != parameters.size()) || (isVarargs && declaredParameters.size() > parameters.size() + 1)){
                    var errorMessage = String.format("Method '%s' expects %d parameters, but got %d", methodName, declaredParameters.size(), parameters.size());
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(memberCallExpr),
                            NodeUtils.getColumn(memberCallExpr),
                            errorMessage,
                            null
                    ));
                }
                else if (!returnType.equals(table.getReturnType(methodName).getName())) {
                    var errorMessage = String.format("Method '%s' expects a return type of '%s', but got '%s'", methodName, table.getReturnType(methodName).getName(), returnType);
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(memberCallExpr),
                            NodeUtils.getColumn(memberCallExpr),
                            errorMessage,
                            null
                    ));
                }
                else {
                    // Check if the types of the parameters are the same
                    for (int i = 0; i < parameters.size(); i++) {
                        var parameterType = Utils.getOperandType(parameters.get(i), table, currentMethod);
                        String declaredParameterType;

                        if (isVarargs && i >= declaredParameters.size() - 1) {
                            declaredParameterType = "int";
                        } else {
                            declaredParameterType = declaredParameters.get(i).getType().getName();
                        }

                        if (!Objects.equals(parameterType, declaredParameterType)) {
                            var errorMessage = String.format("Method '%s' expects parameter %d to be of type '%s', but got '%s'", methodName, i + 1, declaredParameterType, parameterType);
                            addReport(Report.newError(
                                    Stage.SEMANTIC,
                                    NodeUtils.getLine(memberCallExpr),
                                    NodeUtils.getColumn(memberCallExpr),
                                    errorMessage,
                                    null
                            ));
                        }
                    }
                }
            }
        }
        else if (superClass != null && !isSuperClassImported) {
            if (!availableMethods.contains(superClass)) {
                var errorMessage = String.format("Class '%s' does not contain a method '%s'", superClass, methodName);
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(memberCallExpr),
                        NodeUtils.getColumn(memberCallExpr),
                        errorMessage,
                        null
                ));
            }
        } else if (!isClassImported && !isSuperClassImported){
            var errorMessage = String.format("Class '%s' is not imported", objectClassName);
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(memberCallExpr),
                    NodeUtils.getColumn(memberCallExpr),
                    errorMessage,
                    null
            ));
        }




        return null;
    }




}
