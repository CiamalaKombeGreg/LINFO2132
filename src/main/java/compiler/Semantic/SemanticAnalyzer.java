package compiler.Semantic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import compiler.Parser.ASTNode;
import compiler.Parser.AssignmentNode;
import compiler.Parser.BinaryExprNode;
import compiler.Parser.BlockNode;
import compiler.Parser.CallNode;
import compiler.Parser.CollDeclNode;
import compiler.Parser.ExprNode;
import compiler.Parser.ExprStatementNode;
import compiler.Parser.FieldAccessNode;
import compiler.Parser.ForNode;
import compiler.Parser.FunctionDefNode;
import compiler.Parser.IdentifierNode;
import compiler.Parser.IfNode;
import compiler.Parser.IndexAccessNode;
import compiler.Parser.LiteralNode;
import compiler.Parser.ParamNode;
import compiler.Parser.ProgramNode;
import compiler.Parser.ReturnNode;
import compiler.Parser.StatementNode;
import compiler.Parser.UnaryExprNode;
import compiler.Parser.VarDeclNode;
import compiler.Parser.WhileNode;

public class SemanticAnalyzer {
    private final Map<String, Type> typeTable = new HashMap<>(); // Store the types defined in the program, including built-in and user-defined collections
    private final SymbolTable globalSymbols = new SymbolTable(null); // Store global variables and functions, null parent since it's the global scope

    public SemanticAnalyzer() {
        registerBuiltInTypes(); // Initialize the type table with built-in types like INT, FLOAT, STRING, BOOL, VOID
        registerBuiltInFunctions(); // Initialize the global symbol table with built-in functions like read_INT, print, etc.
    }

    public void analyze(ASTNode root) {
        if (!(root instanceof ProgramNode program)) { // Verify that the root of the AST is a ProgramNode from the parser
            throw new SemanticException("ScopeError", "Root AST node must be a ProgramNode");
        }

        firstPass(program);
        secondPass(program);
    }

    private void registerBuiltInTypes() {
        typeTable.put("INT", PrimitiveType.INT);
        typeTable.put("FLOAT", PrimitiveType.FLOAT);
        typeTable.put("STRING", PrimitiveType.STRING);
        typeTable.put("BOOL", PrimitiveType.BOOL);
        typeTable.put("BOOLEAN", PrimitiveType.BOOL); // Parser tolerates BOOLEAN in some places, so we map it to BOOL
        typeTable.put("VOID", PrimitiveType.VOID);
    }

    private void firstPass(ProgramNode program) {
        /* 
         * The first pass of semantic analysis registers all collections, functions, and global variables in the type table and global symbol table.
         * This allows us to resolve types and symbols in the second pass when we analyze function bodies and expressions.
         */
        for (ASTNode node : program.getElements()) {
            if (node instanceof CollDeclNode coll) {
                registerCollection(coll);
            } else if (node instanceof FunctionDefNode fn) {
                registerFunction(fn);
            } else if (node instanceof VarDeclNode varDecl) {
                registerGlobalVar(varDecl);
            }
        }
    }

    private void secondPass(ProgramNode program) {
        for (ASTNode node : program.getElements()) {
            if (node instanceof FunctionDefNode fn) {
                analyzeFunction(fn);
            } else if (node instanceof VarDeclNode varDecl) {
                if (varDecl.getInitializer() != null) {
                    if (varDecl.isFinal() && containsFunctionCall(varDecl.getInitializer())) {
                        throw new SemanticException("ScopeError",
                                "Final global variable '" + varDecl.getName()
                                        + "' cannot use a function call in its initializer");
                    }
                    Type declared = resolveType(varDecl.getType(), "TypeError");
                    Type initType = analyzeExpr(varDecl.getInitializer(), globalSymbols);

                    if (!declared.equals(initType)) {
                        throw new SemanticException("TypeError",
                                "Global variable '" + varDecl.getName()
                                        + "' expects " + declared + " but got " + initType);
                    }
                }
            }
        }
    }

    private void analyzeFunction(FunctionDefNode fn) {
        SymbolTable localScope = new SymbolTable(globalSymbols);

        for (ParamNode param : fn.getParams()) {
            if (localScope.containsInCurrentScope(param.getName())) {
                throw new SemanticException("ScopeError",
                        "Duplicate parameter '" + param.getName()
                                + "' in function '" + fn.getName() + "'");
            }

            Type paramType = resolveType(param.getType(), "ArgumentError");
            localScope.define(new SymbolInfo(
                    param.getName(),
                    paramType,
                    SymbolKind.PARAMETER,
                    false
            ));
        }

        Type expectedReturnType = fn.getReturnType() == null
                ? PrimitiveType.VOID
                : resolveType(fn.getReturnType(), "ReturnError");

        analyzeBlock(fn.getBody(), localScope, expectedReturnType);

        if (!expectedReturnType.equals(PrimitiveType.VOID) && !containsReturn(fn.getBody())) { // if a function is non-void, require at least one return somewhere in its body.
            throw new SemanticException("ReturnError",
                    "Function '" + fn.getName() + "' must contain a return statement");
        }
    }

    private void analyzeBlock(BlockNode block, SymbolTable scope, Type expectedReturnType) {
        SymbolTable blockScope = new SymbolTable(scope);

        for (StatementNode stmt : block.getStatements()) {
            analyzeStatement(stmt, blockScope, expectedReturnType);
        }
    }

    private void analyzeStatement(StatementNode stmt, SymbolTable scope, Type expectedReturnType) {
        if (stmt instanceof BlockNode block) {
            analyzeBlock(block, scope, expectedReturnType);
            return;
        }

        if (stmt instanceof VarDeclNode varDecl) {
            if (scope.containsInCurrentScope(varDecl.getName())) {
                throw new SemanticException("ScopeError",
                        "Duplicate declaration of '" + varDecl.getName() + "'");
            }

            Type declaredType = resolveType(varDecl.getType(), "TypeError");
            scope.define(new SymbolInfo(
                    varDecl.getName(),
                    declaredType,
                    SymbolKind.VARIABLE,
                    varDecl.isFinal()
            ));

            if (varDecl.getInitializer() != null) {
                if (varDecl.isFinal() && containsFunctionCall(varDecl.getInitializer())) {
                    throw new SemanticException("ScopeError",
                            "Final variable '" + varDecl.getName()
                                    + "' cannot use a function call in its initializer");
                }

                Type initType = analyzeExpr(varDecl.getInitializer(), scope);
                if (!declaredType.equals(initType)) {
                    throw new SemanticException("TypeError",
                            "Variable '" + varDecl.getName()
                                    + "' expects " + declaredType + " but got " + initType);
                }
            }
            return;
        }

        if (stmt instanceof ReturnNode ret) {
            if (ret.getExpr() == null) {
                if (!expectedReturnType.equals(PrimitiveType.VOID)) {
                    throw new SemanticException("ReturnError",
                            "Function must return " + expectedReturnType + " but returned nothing");
                }
            } else {
                Type actualType = analyzeExpr(ret.getExpr(), scope);
                if (!expectedReturnType.equals(actualType)) {
                    throw new SemanticException("ReturnError",
                            "Function must return " + expectedReturnType + " but got " + actualType);
                }
            }
            return;
        }

        if (stmt instanceof IfNode ifNode) {
            Type condType = analyzeExpr(ifNode.getCondition(), scope);
            if (!condType.equals(PrimitiveType.BOOL)) {
                throw new SemanticException("MissingConditionError",
                        "If condition must be BOOL, got " + condType);
            }

            analyzeStatement(ifNode.getThenBranch(), new SymbolTable(scope), expectedReturnType);
            if (ifNode.getElseBranch() != null) {
                analyzeStatement(ifNode.getElseBranch(), new SymbolTable(scope), expectedReturnType);
            }
            return;
        }

        if (stmt instanceof WhileNode whileNode) {
            Type condType = analyzeExpr(whileNode.getCondition(), scope);
            if (!condType.equals(PrimitiveType.BOOL)) {
                throw new SemanticException("MissingConditionError",
                        "While condition must be BOOL, got " + condType);
            }

            analyzeStatement(whileNode.getBody(), new SymbolTable(scope), expectedReturnType);
            return;
        }

        if (stmt instanceof ForNode forNode) {
            SymbolTable forScope = new SymbolTable(scope);

            if (forNode.getInit() instanceof StatementNode initStmt) {
                analyzeStatement(initStmt, forScope, expectedReturnType);
            } else if (forNode.getInit() instanceof ExprNode initExpr) {
                analyzeExpr(initExpr, forScope);
            }

            if (forNode.getCondition() != null) {
                Type condType = analyzeExpr(forNode.getCondition(), forScope);
                if (!condType.equals(PrimitiveType.BOOL)) {
                    throw new SemanticException("MissingConditionError",
                            "For condition must be BOOL, got " + condType);
                }
            }

            if (forNode.getUpdate() instanceof StatementNode updateStmt) {
                analyzeStatement(updateStmt, forScope, expectedReturnType);
            } else if (forNode.getUpdate() instanceof ExprNode updateExpr) {
                analyzeExpr(updateExpr, forScope);
            }

            analyzeStatement(forNode.getBody(), forScope, expectedReturnType);
            return;
        }

        if (stmt instanceof AssignmentNode assignment) {
            ensureAssignableTarget(assignment.getTarget(), scope);

            Type targetType = analyzeExpr(assignment.getTarget(), scope);
            Type valueType = analyzeExpr(assignment.getValue(), scope);

            if (!targetType.equals(valueType)) {
                throw new SemanticException("TypeError",
                        "Assignment expects " + targetType + " but got " + valueType);
            }
            return;
        }

        if (stmt instanceof ExprStatementNode exprStmt) {
            analyzeExpr(exprStmt.getExpr(), scope);
        }
    }

    private Type analyzeExpr(ExprNode expr, SymbolTable scope) {
        if (expr instanceof IdentifierNode id) {
            SymbolInfo info = scope.resolve(id.getName());
            if (info == null) {
                throw new SemanticException("ScopeError",
                        "Unknown identifier '" + id.getName() + "'");
            }
            return info.getType();
        }

        if (expr instanceof LiteralNode lit) {
            return switch (lit.getKind()) {
                case "Integer" -> PrimitiveType.INT;
                case "Float" -> PrimitiveType.FLOAT;
                case "String" -> PrimitiveType.STRING;
                case "Boolean" -> PrimitiveType.BOOL;
                default -> throw new SemanticException("TypeError",
                        "Unknown literal kind: " + lit.getKind());
            };
        }

        // Analyze binary expressions such as +, -, *, /, %, ==, =/=, <, >, <=, >=, &&, ||, ->
        if (expr instanceof BinaryExprNode bin) {
            Type leftType = analyzeExpr(bin.getLeft(), scope);
            Type rightType = analyzeExpr(bin.getRight(), scope);
            String op = bin.getOperator();

            switch (op) {
                case "+", "-", "*", "/", "%" -> {
                    if (!isNumericType(leftType) || !isNumericType(rightType)) { // Arithmetic operators only work on numeric types.
                        throw new SemanticException("OperatorError",
                                "Operator '" + op + "' requires numeric operands, got "
                                        + leftType + " and " + rightType);
                    }

                    if (!leftType.equals(rightType)) { // Same numeric type for arithmetic operations is the safest interpretation of the project statement.
                        throw new SemanticException("OperatorError",
                                "Operator '" + op + "' requires the same numeric type on both sides, got "
                                        + leftType + " and " + rightType);
                    }

                    return leftType;
                }

                case "&&", "||" -> {
                    if (!leftType.equals(PrimitiveType.BOOL) || !rightType.equals(PrimitiveType.BOOL)) {
                        throw new SemanticException("OperatorError",
                                "Operator '" + op + "' requires BOOL operands, got "
                                        + leftType + " and " + rightType);
                    }
                    return PrimitiveType.BOOL;
                }

                case "==", "=/=" -> {
                    if (!leftType.equals(rightType)) {
                        throw new SemanticException("OperatorError",
                                "Operator '" + op + "' requires operands of the same type, got "
                                        + leftType + " and " + rightType);
                    }
                    return PrimitiveType.BOOL;
                }

                case "<", ">", "<=", ">=" -> {
                    if (!isNumericType(leftType) || !isNumericType(rightType)) {
                        throw new SemanticException("OperatorError",
                                "Comparison operator '" + op + "' requires numeric operands, got "
                                        + leftType + " and " + rightType);
                    }

                    if (!leftType.equals(rightType)) {
                        throw new SemanticException("OperatorError",
                                "Comparison operator '" + op + "' requires the same numeric type on both sides, got "
                                        + leftType + " and " + rightType);
                    }

                    return PrimitiveType.BOOL;
                }

                case "->" -> {
                    // Range expressions should operate on numeric values of the same type.
                    if (!isNumericType(leftType) || !isNumericType(rightType)) {
                        throw new SemanticException("OperatorError",
                                "Operator '->' requires numeric operands, got "
                                        + leftType + " and " + rightType);
                    }

                    if (!leftType.equals(rightType)) {
                        throw new SemanticException("OperatorError",
                                "Operator '->' requires the same numeric type on both sides, got "
                                        + leftType + " and " + rightType);
                    }

                    // The parser uses '->' mostly in for conditions. We return BOOL so it can be accepted as a condition.
                    return PrimitiveType.BOOL;
                }

                default -> throw new SemanticException("OperatorError",
                        "Unknown binary operator: " + op);
            }
        }

        // Analyze unary expressions such as -x and not x
        if (expr instanceof UnaryExprNode un) {
            Type innerType = analyzeExpr(un.getExpr(), scope);
            String op = un.getOperator();

            switch (op) {
                case "-" -> {
                    if (!isNumericType(innerType)) {
                        throw new SemanticException("OperatorError",
                                "Unary operator '-' requires a numeric operand, got " + innerType);
                    }
                    return innerType;
                }

                case "not" -> {
                    if (!innerType.equals(PrimitiveType.BOOL)) {
                        throw new SemanticException("OperatorError",
                                "Unary operator 'not' requires a BOOL operand, got " + innerType);
                    }
                    return PrimitiveType.BOOL;
                }

                default -> throw new SemanticException("OperatorError",
                        "Unknown unary operator: " + op);
            }
        }

        // Analyze function calls and collection constructors
        if (expr instanceof CallNode call) {
            ExprNode callee = call.getCallee();

            // Special array constructor emitted by the parser (e.g. INT ARRAY [5])
            if (callee instanceof IdentifierNode special && special.getName().endsWith(" ARRAY")) {
                if (call.getArgs().size() != 1) {
                    throw new SemanticException("ArgumentError",
                            "Array constructor expects exactly one size argument");
                }

                Type sizeType = analyzeExpr(call.getArgs().get(0), scope);
                if (!sizeType.equals(PrimitiveType.INT)) {
                    throw new SemanticException("ArgumentError",
                            "Array constructor size must be INT, got " + sizeType);
                }

                String baseTypeName = special.getName().substring(0, special.getName().length() - " ARRAY".length());
                Type baseType = resolveType(baseTypeName, "ArgumentError");
                return new ArrayType(baseType);
            }

            // Normal function call or collection constructor through an identifier
            if (callee instanceof IdentifierNode id) {
                String name = id.getName();

                if ("print".equals(name) || "println".equals(name)) {
                    if (call.getArgs().size() > 1) {
                        throw new SemanticException("ArgumentError",
                                "Built-in function '" + name + "' accepts at most one argument");
                    }

                    if (call.getArgs().size() == 1) {
                        analyzeExpr(call.getArgs().get(0), scope);
                    }

                    return PrimitiveType.VOID;
                }

                // Call to a normal function
                SymbolInfo info = scope.resolve(name);
                if (info != null && info.getType() instanceof FunctionType fnType) {
                    List<Type> expected = fnType.getParameterTypes();
                    List<ExprNode> actualArgs = call.getArgs();

                    if (expected.size() != actualArgs.size()) {
                        throw new SemanticException("ArgumentError",
                                "Function '" + name + "' expects " + expected.size()
                                        + " arguments but got " + actualArgs.size());
                    }

                    for (int i = 0; i < expected.size(); i++) {
                        Type actualType = analyzeExpr(actualArgs.get(i), scope);
                        if (!expected.get(i).equals(actualType)) {
                            throw new SemanticException("ArgumentError",
                                    "Function '" + name + "' expects argument " + (i + 1)
                                            + " of type " + expected.get(i) + " but got " + actualType);
                        }
                    }

                    return fnType.getReturnType();
                }

                // Collection constructor call (e.g. Person(...))
                Type type = typeTable.get(name);
                if (type instanceof CollectionType collType) {
                    List<ExprNode> actualArgs = call.getArgs();
                    List<Type> fieldTypes = new ArrayList<>(collType.getFields().values());

                    if (fieldTypes.size() != actualArgs.size()) {
                        throw new SemanticException("ArgumentError",
                                "Collection constructor '" + name + "' expects " + fieldTypes.size()
                                        + " arguments but got " + actualArgs.size());
                    }

                    for (int i = 0; i < fieldTypes.size(); i++) {
                        Type actualType = analyzeExpr(actualArgs.get(i), scope);
                        if (!fieldTypes.get(i).equals(actualType)) {
                            throw new SemanticException("ArgumentError",
                                    "Collection constructor '" + name + "' expects argument " + (i + 1)
                                            + " of type " + fieldTypes.get(i) + " but got " + actualType);
                        }
                    }

                    return collType;
                }

                throw new SemanticException("ArgumentError",
                        "Callee '" + name + "' is neither a function nor a collection constructor");
            }

            throw new SemanticException("ArgumentError",
                    "Unsupported callee in function/constructor call");
        }

        // Analyze field access: x.field
        if (expr instanceof FieldAccessNode fieldAccess) {
            Type targetType = analyzeExpr(fieldAccess.getTarget(), scope);

            if (!(targetType instanceof CollectionType collType)) {
                throw new SemanticException("TypeError",
                        "Field access requires a collection type, got " + targetType);
            }

            if (!collType.hasField(fieldAccess.getFieldName())) {
                throw new SemanticException("ScopeError",
                        "Unknown field '" + fieldAccess.getFieldName()
                                + "' for collection type '" + collType.getName() + "'");
            }

            return collType.getFieldType(fieldAccess.getFieldName());
        }

        // Analyze array index access (x[i])
        if (expr instanceof IndexAccessNode indexAccess) {
            Type targetType = analyzeExpr(indexAccess.getTarget(), scope);
            Type indexType = analyzeExpr(indexAccess.getIndex(), scope);

            if (!(targetType instanceof ArrayType arrayType)) {
                throw new SemanticException("TypeError",
                        "Index access requires an array target, got " + targetType);
            }

            if (!indexType.equals(PrimitiveType.INT)) {
                throw new SemanticException("TypeError",
                        "Array index must be INT, got " + indexType);
            }

            return arrayType.getElementType();
        }

        // we only want function scopes + basic identifier/literal checks working first.
        throw new SemanticException("TypeError",
                "Expression analysis not implemented yet for " + expr.getClass().getSimpleName());
    }

    private boolean containsFunctionCall(ExprNode expr) {
        if (expr == null) {
            return false;
        }

        if (expr instanceof CallNode) {
            return true;
        }

        if (expr instanceof BinaryExprNode bin) {
            return containsFunctionCall(bin.getLeft()) || containsFunctionCall(bin.getRight());
        }

        if (expr instanceof UnaryExprNode un) {
            return containsFunctionCall(un.getExpr());
        }

        if (expr instanceof FieldAccessNode field) {
            return containsFunctionCall(field.getTarget());
        }

        if (expr instanceof IndexAccessNode index) {
            return containsFunctionCall(index.getTarget()) || containsFunctionCall(index.getIndex());
        }

        return false;
    }

    private boolean isNumericType(Type type) {
        return type.equals(PrimitiveType.INT) || type.equals(PrimitiveType.FLOAT);
    }

    private void ensureAssignableTarget(ExprNode target, SymbolTable scope) {
        // Assignment to a plain identifier: x = ...
        if (target instanceof IdentifierNode id) {
            SymbolInfo info = scope.resolve(id.getName());
            if (info == null) {
                throw new SemanticException("ScopeError",
                        "Unknown identifier '" + id.getName() + "'");
            }

            if (info.isFinal()) {
                throw new SemanticException("TypeError",
                        "Cannot assign to final variable '" + id.getName() + "'");
            }
            return;
        }

        // Assignment to an array element: x[i] = ...
        if (target instanceof IndexAccessNode indexAccess) {
            analyzeExpr(indexAccess, scope); // The target expression itself must be valid and indexable.

            if (indexAccess.getTarget() instanceof IdentifierNode id) { // If the base is an identifier, we can also forbid writes to final arrays.
                SymbolInfo info = scope.resolve(id.getName());
                if (info != null && info.isFinal()) {
                    throw new SemanticException("TypeError",
                            "Cannot assign through final variable '" + id.getName() + "'");
                }
            }
            return;
        }

        // Assignment to a field: x.field = ...
        if (target instanceof FieldAccessNode fieldAccess) {
            analyzeExpr(fieldAccess, scope); // The field access must be semantically valid.

            if (fieldAccess.getTarget() instanceof IdentifierNode id) { // If the base object is directly a final identifier, reject assignment through it.
                SymbolInfo info = scope.resolve(id.getName());
                if (info != null && info.isFinal()) {
                    throw new SemanticException("TypeError",
                            "Cannot assign through final variable '" + id.getName() + "'");
                }
            }
            return;
        }

        throw new SemanticException("TypeError",
                "Invalid assignment target");
    }

    private boolean containsReturn(StatementNode stmt) {
        if (stmt instanceof ReturnNode) {
            return true;
        }

        if (stmt instanceof BlockNode block) {
            for (StatementNode inner : block.getStatements()) {
                if (containsReturn(inner)) {
                    return true;
                }
            }
            return false;
        }

        if (stmt instanceof IfNode ifNode) {
            boolean thenHasReturn = containsReturn(ifNode.getThenBranch());
            boolean elseHasReturn = ifNode.getElseBranch() != null && containsReturn(ifNode.getElseBranch());
            return thenHasReturn || elseHasReturn;
        }

        if (stmt instanceof WhileNode whileNode) {
            return containsReturn(whileNode.getBody());
        }

        if (stmt instanceof ForNode forNode) {
            return containsReturn(forNode.getBody());
        }

        return false;
    }

    private void registerBuiltInFunctions() {
        globalSymbols.define(new SymbolInfo(
                "read_INT",
                new FunctionType(List.of(), PrimitiveType.INT),
                SymbolKind.FUNCTION,
                true
        ));

        globalSymbols.define(new SymbolInfo(
                "read_FLOAT",
                new FunctionType(List.of(), PrimitiveType.FLOAT),
                SymbolKind.FUNCTION,
                true
        ));

        globalSymbols.define(new SymbolInfo(
                "read_STRING",
                new FunctionType(List.of(), PrimitiveType.STRING),
                SymbolKind.FUNCTION,
                true
        ));

        globalSymbols.define(new SymbolInfo(
                "print_INT",
                new FunctionType(List.of(PrimitiveType.INT), PrimitiveType.VOID),
                SymbolKind.FUNCTION,
                true
        ));

        globalSymbols.define(new SymbolInfo(
                "print_FLOAT",
                new FunctionType(List.of(PrimitiveType.FLOAT), PrimitiveType.VOID),
                SymbolKind.FUNCTION,
                true
        ));

        globalSymbols.define(new SymbolInfo( // Registered too, but handled specially in analyzeExpr(CallNode ...)
                "print",
                new FunctionType(List.of(), PrimitiveType.VOID),
                SymbolKind.FUNCTION,
                true
        ));

        globalSymbols.define(new SymbolInfo(
                "println",
                new FunctionType(List.of(), PrimitiveType.VOID),
                SymbolKind.FUNCTION,
                true
        ));
    }

    private void registerCollection(CollDeclNode coll) {
        String name = coll.getName();

        if (typeTable.containsKey(name)) { // Check if the collection name already exists in the type table
            throw new SemanticException("CollectionError",
                    "Collection type '" + name + "' overwrites an existing type");
        }

        if (name.isEmpty() || !Character.isUpperCase(name.charAt(0))) { // Enforce the convention that collection names must start with a capital letter
            throw new SemanticException("CollectionError",
                    "Collection name must begin with a capital letter: " + name);
        }

        CollectionType collType = new CollectionType(name);

        for (compiler.Parser.FieldNode field : coll.getFields()) { // Process each field in the collection declaration
            if (collType.hasField(field.getName())) { // Reject duplicate field names within the same collection
                throw new SemanticException("CollectionError",
                        "Duplicate field '" + field.getName() + "' in collection '" + name + "'");
            }

            Type fieldType = resolveType(field.getType(), "CollectionError");
            collType.addField(field.getName(), fieldType);
        }

        typeTable.put(name, collType); // Register the new collection type in the type table
    }

    private void registerFunction(FunctionDefNode fn) {
        String name = fn.getName(); // Get the function name from the AST node

        if (globalSymbols.containsInCurrentScope(name)) { // Check if the function name already exists in the global symbol table
            throw new SemanticException("ScopeError",
                    "Duplicate global declaration of '" + name + "'");
        }

        Type returnType = fn.getReturnType() == null
                ? PrimitiveType.VOID
                : resolveType(fn.getReturnType(), "ReturnError"); // If the function has no return type specified, we treat it as VOID.

        List<Type> parameterTypes = new ArrayList<>();
        for (compiler.Parser.ParamNode param : fn.getParams()) { // Process each parameter in the function declaration
            parameterTypes.add(resolveType(param.getType(), "ArgumentError")); // If the parameter type is unknown, resolveType will throw an error with the "ArgumentError" keyword
        }

        FunctionType fnType = new FunctionType(parameterTypes, returnType);
        globalSymbols.define(new SymbolInfo(name, fnType, SymbolKind.FUNCTION, true)); // Register the function in the global symbol table. Functions are always final (cannot be reassigned) and have the kind FUNCTION.
    }

    private void registerGlobalVar(VarDeclNode varDecl) {
        String name = varDecl.getName();

        if (globalSymbols.containsInCurrentScope(name)) { // Check if the variable name already exists in the global symbol table. This prevents duplicate global variable declarations (and by extension, global variables that would shadow functions or collections)
            throw new SemanticException("ScopeError",
                    "Duplicate global declaration of '" + name + "'");
        }

        Type type = resolveType(varDecl.getType(), "TypeError"); // Resolve the variable's type. If the type is unknown, resolveType will throw an error with the "TypeError" keyword
        globalSymbols.define(new SymbolInfo(name, type, SymbolKind.VARIABLE, varDecl.isFinal())); // Add the variable to the global symbol table.
    }

    private Type resolveType(String typeName, String keyword) {
        if (typeName.startsWith("ARRAY[")) { // If the type name starts with "ARRAY[", we treat it as an array type.
            String inner = typeName.substring(6, typeName.length() - 1); // Extract the inner type name from the array type declaration
            return new ArrayType(resolveType(inner, keyword));
        }

        Type type = typeTable.get(typeName); // Look up the type name in the type table. This will find built-in types like INT, FLOAT, STRING, BOOL, VOID, as well as any user-defined collection types that were registered in the first pass.
        if (type == null) { // If the type is not found in the type table, we throw a semantic exception indicating that the type is unknown.
            throw new SemanticException(keyword, "Unknown type: " + typeName);
        }
        return type;
    }

    public Map<String, Type> getTypeTable() { // For testing and debugging purposes
        return typeTable;
    }

    public SymbolTable getGlobalSymbols() { // For testing and debugging purposes
        return globalSymbols;
    }
}