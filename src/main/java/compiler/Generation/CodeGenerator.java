package compiler.Generation;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.FADD;
import static org.objectweb.asm.Opcodes.FCMPL;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FDIV;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FMUL;
import static org.objectweb.asm.Opcodes.FREM;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.FSUB;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.IALOAD;
import static org.objectweb.asm.Opcodes.IASTORE;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.ICONST_M1;
import static org.objectweb.asm.Opcodes.IDIV;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFGE;
import static org.objectweb.asm.Opcodes.IFGT;
import static org.objectweb.asm.Opcodes.IFLE;
import static org.objectweb.asm.Opcodes.IFLT;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IF_ICMPEQ;
import static org.objectweb.asm.Opcodes.IF_ICMPGE;
import static org.objectweb.asm.Opcodes.IF_ICMPGT;
import static org.objectweb.asm.Opcodes.IF_ICMPLE;
import static org.objectweb.asm.Opcodes.IF_ICMPLT;
import static org.objectweb.asm.Opcodes.IF_ICMPNE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IMUL;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IREM;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.ISUB;
import static org.objectweb.asm.Opcodes.NEWARRAY;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SIPUSH;
import static org.objectweb.asm.Opcodes.T_INT;
import static org.objectweb.asm.Opcodes.V17;

import compiler.Parser.ASTNode;
import compiler.Parser.AssignmentNode;
import compiler.Parser.BinaryExprNode;
import compiler.Parser.BlockNode;
import compiler.Parser.CallNode;
import compiler.Parser.ExprNode;
import compiler.Parser.ExprStatementNode;
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
import compiler.Parser.VarDeclNode;
import compiler.Parser.WhileNode;

public class CodeGenerator {

    private final String className;
    private final Map<String, FunctionDefNode> functions = new HashMap<>();

    public CodeGenerator(String className) {
        this.className = className;
    }

    public void generate(ProgramNode program, String outputPath) throws IOException {

        // ASM helper that builds the bytecode of the class.
        // COMPUTE_FRAMES and COMPUTE_MAXS let ASM calculate stack frames and stack/local sizes automatically.
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        // Create the class header.
        cw.visit(
                V17,
                ACC_PUBLIC | ACC_SUPER,
                className,
                null,
                "java/lang/Object",
                null
        );

        // Every JVM class needs a constructor.
        generateConstructor(cw);

        // Register all functions before generation.
        for (ASTNode node : program.getElements()) {
            if (node instanceof FunctionDefNode fn) {
                functions.put(fn.getName(), fn);
            }
        }

        // Generate all functions.
        for (ASTNode node : program.getElements()) {
            if (node instanceof FunctionDefNode fn) {
                generateFunction(cw, fn);
            }
        }

        cw.visitEnd();

        // Write the generated class bytes into the requested .class file.
        try (FileOutputStream out = new FileOutputStream(outputPath)) {
            out.write(cw.toByteArray());
        }
    }

    private void generateConstructor(ClassWriter cw) {

        MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "<init>",
                "()V",
                null,
                null
        );

        mv.visitCode();

        // Load "this" onto the stack.
        mv.visitVarInsn(ALOAD, 0);

        // Call Object constructor.
        mv.visitMethodInsn(
                INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V",
                false
        );

        // End constructor.
        mv.visitInsn(RETURN);

        // ASM computes stack/local sizes automatically because of COMPUTE_MAXS.
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    // Generate: public static void main(String[] args)
    private void generateMain(ClassWriter cw, FunctionDefNode fn) {

        MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC | ACC_STATIC,
                "main",
                "([Ljava/lang/String;)V",
                null,
                null
        );

        mv.visitCode();

        // main is static and has String[] args in slot 0.
        // Our first user variable therefore starts at slot 1.
        CodeGenContext ctx = new CodeGenContext(1);

        // Generate all statements inside the block.
        generateBlock(mv, fn.getBody(), ctx);

        // End main function.
        mv.visitInsn(RETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    // Generate a user-defined function.
    private void generateFunction(ClassWriter cw, FunctionDefNode fn) {

        // JVM main has a special signature.
        if ("main".equals(fn.getName())) {
            generateMain(cw, fn);
            return;
        }

        MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC | ACC_STATIC,
                fn.getName(),
                methodDescriptor(fn),
                null,
                null
        );

        mv.visitCode();

        // Static methods start at slot 0.
        CodeGenContext ctx = new CodeGenContext(0);

        // Register parameters as local variables.
        for (ParamNode param : fn.getParams()) {
            ctx.declareLocal(
                    param.getName(),
                    normalizeType(param.getType())
            );
        }

        // Generate function body.
        generateBlock(mv, fn.getBody(), ctx);

        // Safety return for VOID functions.
        if (fn.getReturnType() == null || "VOID".equals(fn.getReturnType())) {
            mv.visitInsn(RETURN);
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    // Generate all statements of a block one after another.
    private void generateBlock(MethodVisitor mv, BlockNode block, CodeGenContext ctx) {
        for (StatementNode stmt : block.getStatements()) {
            generateStatement(mv, stmt, ctx);
        }
    }

    // Generate one statement.
    private void generateStatement(MethodVisitor mv, StatementNode stmt, CodeGenContext ctx) {

        // Variable declaration, for example: INT x = 3;
        if (stmt instanceof VarDeclNode varDecl) {
            LocalVar local = ctx.declareLocal(varDecl.getName(), normalizeType(varDecl.getType()));

            // If there is an initializer, generate its value.
            if (varDecl.getInitializer() != null) {
                generateExpr(mv, varDecl.getInitializer(), ctx);
            } else {
                // If no initializer exists, push a default value depending on the variable type.
                generateDefaultValue(mv, local.getType());
            }

            // Store the value from the stack into the local variable slot.
            storeLocal(mv, local);
            return;
        }

        // Assignment, for example: x = x + 1; or a[0] = 10;
        if (stmt instanceof AssignmentNode assignment) {

            if (assignment.getTarget() instanceof IndexAccessNode index) {
                generateExpr(mv, index.getTarget(), ctx);
                generateExpr(mv, index.getIndex(), ctx);
                generateExpr(mv, assignment.getValue(), ctx);

                String targetType = inferExprType(index.getTarget(), ctx);

                if ("ARRAY[INT]".equals(targetType)) {
                    mv.visitInsn(IASTORE);
                    return;
                }

                mv.visitInsn(AASTORE);
                return;
            }

            if (assignment.getTarget() instanceof IdentifierNode id) {
                LocalVar local = ctx.resolveLocal(id.getName());

                if (local == null) {
                    throw new RuntimeException("Unknown local variable: " + id.getName());
                }

                generateExpr(mv, assignment.getValue(), ctx);
                storeLocal(mv, local);
                return;
            }

            throw new RuntimeException("Unsupported assignment target: "
                    + assignment.getTarget().getClass().getSimpleName());
        }

        // Expression statement, for example: println(x);
        if (stmt instanceof ExprStatementNode exprStmt) {
            generateExpr(mv, exprStmt.getExpr(), ctx);

            // If the expression leaves a value on the stack, remove it.
            // Calls like println return VOID, so nothing is popped in that case.
            String type = inferExprType(exprStmt.getExpr(), ctx);
            if (!"VOID".equals(type)) {
                mv.visitInsn(POP);
            }
            return;
        }

        // If statement.
        if (stmt instanceof IfNode ifNode) {
            Label elseLabel = new Label();
            Label endLabel = new Label();

            // If condition is false, jump to else.
            generateConditionJump(mv, ifNode.getCondition(), ctx, false, elseLabel);

            // Then branch.
            generateStatement(mv, ifNode.getThenBranch(), ctx);

            // After then branch, skip else branch.
            mv.visitJumpInsn(GOTO, endLabel);

            // Else branch.
            mv.visitLabel(elseLabel);
            if (ifNode.getElseBranch() != null) {
                generateStatement(mv, ifNode.getElseBranch(), ctx);
            }

            // End of if.
            mv.visitLabel(endLabel);
            return;
        }

        // While loop.
        if (stmt instanceof WhileNode whileNode) {
            Label startLabel = new Label();
            Label endLabel = new Label();

            // Start of loop condition.
            mv.visitLabel(startLabel);

            // If condition is false, leave loop.
            generateConditionJump(mv, whileNode.getCondition(), ctx, false, endLabel);

            // Loop body.
            generateStatement(mv, whileNode.getBody(), ctx);

            // Go back to condition.
            mv.visitJumpInsn(GOTO, startLabel);

            // End of loop.
            mv.visitLabel(endLabel);
            return;
        }

        // Nested block.
        if (stmt instanceof BlockNode block) {
            generateBlock(mv, block, ctx);
            return;
        }

        // For loop.
        if (stmt instanceof ForNode forNode) {
            Label startLabel = new Label();
            Label endLabel = new Label();

            // Generate init part.
            if (forNode.getInit() instanceof StatementNode initStmt) {
                generateStatement(mv, initStmt, ctx);
            } else if (forNode.getInit() instanceof ExprNode initExpr) {
                generateExpr(mv, initExpr, ctx);

                String initType = inferExprType(initExpr, ctx);
                if (!"VOID".equals(initType)) {
                    mv.visitInsn(POP);
                }
            }

            if (forNode.getCondition() instanceof BinaryExprNode bin && "->".equals(bin.getOperator())) {
                LocalVar loopVar = getForLoopVariable(forNode, ctx);
                generateExpr(mv, bin.getLeft(), ctx);
                storeLocal(mv, loopVar);
            }

            // Start of loop condition.
            mv.visitLabel(startLabel);

            // If condition is false, exit loop.
            if (forNode.getCondition() != null) {
                generateForConditionJump(mv, forNode, ctx, endLabel);
            }

            // Body.
            generateStatement(mv, forNode.getBody(), ctx);

            // Update part.
            if (forNode.getUpdate() instanceof StatementNode updateStmt) {
                generateStatement(mv, updateStmt, ctx);
            } else if (forNode.getUpdate() instanceof ExprNode updateExpr) {
                generateForUpdate(mv, forNode, updateExpr, ctx);
            }

            // Go back to condition.
            mv.visitJumpInsn(GOTO, startLabel);

            // End of loop.
            mv.visitLabel(endLabel);
            return;
        }

        // Return statement.
        if (stmt instanceof ReturnNode ret) {

            // return;
            if (ret.getExpr() == null) {
                mv.visitInsn(RETURN);
                return;
            }

            // Generate returned value.
            generateExpr(mv, ret.getExpr(), ctx);

            String type = inferExprType(ret.getExpr(), ctx);

            switch (type) {
                case "INT", "BOOL" -> mv.visitInsn(IRETURN);
                case "FLOAT" -> mv.visitInsn(FRETURN);
                default -> mv.visitInsn(ARETURN);
            }

            return;
        }

        // Assignment.
        if (stmt instanceof AssignmentNode assign) {

            // Array element assignment.
            if (assign.getTarget() instanceof IndexAccessNode index) {

                // Load array reference.
                generateExpr(mv, index.getTarget(), ctx);

                // Load index.
                generateExpr(mv, index.getIndex(), ctx);

                // Load assigned value.
                generateExpr(mv, assign.getValue(), ctx);

                String targetType = inferExprType(index.getTarget(), ctx);

                if ("ARRAY[INT]".equals(targetType)) {
                    mv.visitInsn(IASTORE);
                    return;
                }

                mv.visitInsn(AASTORE);
                return;
            }

            // Normal variable assignment.
            if (assign.getTarget() instanceof IdentifierNode id) {

                LocalVar local = ctx.resolveLocal(id.getName());

                if (local == null) {
                    throw new RuntimeException(
                            "Unknown local variable: " + id.getName()
                    );
                }

                generateExpr(mv, assign.getValue(), ctx);

                storeLocal(mv, local);
                return;
            }
        }

        throw new RuntimeException(
                "Step 5 code generation not implemented yet for statement: "
                        + stmt.getClass().getSimpleName()
        );
    }

    // Generate expressions.
    private void generateExpr(MethodVisitor mv, ExprNode expr, CodeGenContext ctx) {

        // Literal value.
        if (expr instanceof LiteralNode literal) {
            generateLiteral(mv, literal);
            return;
        }

        // Variable access.
        if (expr instanceof IdentifierNode id) {
            LocalVar local = ctx.resolveLocal(id.getName());
            if (local == null) {
                throw new RuntimeException("Unknown local variable: " + id.getName());
            }

            // Push the variable value onto the JVM stack.
            loadLocal(mv, local);
            return;
        }

        // Binary expression, for example: x + y.
        if (expr instanceof BinaryExprNode bin) {
            generateBinaryExpr(mv, bin, ctx);
            return;
        }

        // Function call.
        if (expr instanceof CallNode call) {
            generateCall(mv, call, ctx);
            return;
        }

        // Array indexing.
        if (expr instanceof IndexAccessNode index) {

            // Load array reference.
            generateExpr(mv, index.getTarget(), ctx);

            // Load index.
            generateExpr(mv, index.getIndex(), ctx);

            String targetType = inferExprType(index.getTarget(), ctx);

            if ("ARRAY[INT]".equals(targetType)) {
                mv.visitInsn(IALOAD);
                return;
            }

            mv.visitInsn(AALOAD);
            return;
        }

        throw new RuntimeException(
                "Step 5 code generation not implemented yet for expression: "
                        + expr.getClass().getSimpleName()
        );
    }

    // Generate a binary expression.
    private void generateBinaryExpr(MethodVisitor mv, BinaryExprNode bin, CodeGenContext ctx) {
        String type = inferExprType(bin.getLeft(), ctx);
        String op = bin.getOperator();

        if (isComparisonOperator(op)) {
            generateComparisonExpr(mv, bin, ctx);
            return;
        }

        if ("&&".equals(op) || "||".equals(op)) {
            generateLogicalExpr(mv, bin, ctx);
            return;
        }

        // Generate left operand first.
        generateExpr(mv, bin.getLeft(), ctx);

        // Generate right operand second.
        generateExpr(mv, bin.getRight(), ctx);

        // Integer and boolean values are represented with int bytecode instructions.
        if ("INT".equals(type) || "BOOL".equals(type)) {
            switch (op) {
                case "+" -> mv.visitInsn(IADD);
                case "-" -> mv.visitInsn(ISUB);
                case "*" -> mv.visitInsn(IMUL);
                case "/" -> mv.visitInsn(IDIV);
                case "%" -> mv.visitInsn(IREM);
                default -> throw new RuntimeException("Step 5 only supports arithmetic operators, got: " + op);
            }
            return;
        }

        // Float operations use float bytecode instructions.
        if ("FLOAT".equals(type)) {
            switch (op) {
                case "+" -> mv.visitInsn(FADD);
                case "-" -> mv.visitInsn(FSUB);
                case "*" -> mv.visitInsn(FMUL);
                case "/" -> mv.visitInsn(FDIV);
                case "%" -> mv.visitInsn(FREM);
                default -> throw new RuntimeException("Step 5 only supports arithmetic operators, got: " + op);
            }
            return;
        }

        throw new RuntimeException("Unsupported binary expression type: " + type);
    }

    // Generate a function call.
    private void generateCall(MethodVisitor mv, CallNode call, CodeGenContext ctx) {

        if (!(call.getCallee() instanceof IdentifierNode id)) {
            throw new RuntimeException("Only simple function calls are supported for now");
        }

        String name = id.getName();

        // Array constructor: INT ARRAY [size]
        if ("INT ARRAY".equals(name)) {

            if (call.getArgs().size() != 1) {
                throw new RuntimeException("INT ARRAY expects one size argument");
            }

            // Generate array size.
            generateExpr(mv, call.getArgs().get(0), ctx);

            // Create int array.
            mv.visitIntInsn(NEWARRAY, T_INT);

            return;
        }

        // Handle println(...)
        if ("println".equals(name)) {

            // Load System.out onto the JVM stack.
            mv.visitFieldInsn(
                    GETSTATIC,
                    "java/lang/System",
                    "out",
                    "Ljava/io/PrintStream;"
            );

            // println() with no argument.
            if (call.getArgs().isEmpty()) {
                mv.visitMethodInsn(
                        INVOKEVIRTUAL,
                        "java/io/PrintStream",
                        "println",
                        "()V",
                        false
                );
                return;
            }

            // println(x) with one argument.
            if (call.getArgs().size() == 1) {
                ExprNode arg = call.getArgs().get(0);

                // Determine the argument type before generation.
                String argType = inferExprType(arg, ctx);

                // Generate the argument value first.
                generateExpr(mv, arg, ctx);

                // Call the correct PrintStream.println overload based on the argument type.
                mv.visitMethodInsn(
                        INVOKEVIRTUAL,
                        "java/io/PrintStream",
                        "println",
                        "(" + descriptor(argType) + ")V",
                        false
                );
                return;
            }

            throw new RuntimeException("println accepts zero or one argument");
        }

        // User-defined function call.
        for (ExprNode arg : call.getArgs()) {
            generateExpr(mv, arg, ctx);
        }

        mv.visitMethodInsn(
                INVOKESTATIC,
                className,
                name,
                inferCallDescriptor(call, ctx),
                false
        );

        return;
    }

    // Generate literal values.
    private void generateLiteral(MethodVisitor mv, LiteralNode literal) {

        switch (literal.getKind()) {
            // Push an integer constant onto the JVM stack.
            case "Integer" -> pushInt(mv, Integer.parseInt(literal.getValue()));

            // Push a float constant onto the JVM stack.
            case "Float" -> mv.visitLdcInsn(Float.parseFloat(literal.getValue()));

            // Push a string constant onto the JVM stack.
            case "String" -> mv.visitLdcInsn(literal.getValue());

            // Push a boolean as 0 or 1 onto the JVM stack.
            case "Boolean" -> mv.visitInsn(Boolean.parseBoolean(literal.getValue()) ? ICONST_1 : ICONST_0);

            default -> throw new RuntimeException("Unknown literal kind: " + literal.getKind());
        }
    }

    // Push a default value for an uninitialized local variable.
    private void generateDefaultValue(MethodVisitor mv, String type) {
        switch (type) {
            case "INT", "BOOL" -> mv.visitInsn(ICONST_0);
            case "FLOAT" -> mv.visitInsn(FCONST_0);
            case "STRING" -> mv.visitInsn(ACONST_NULL);
            default -> mv.visitInsn(ACONST_NULL);
        }
    }

    // Push an integer using the smallest convenient JVM instruction.
    private void pushInt(MethodVisitor mv, int value) {
        switch (value) {
            case -1 -> mv.visitInsn(ICONST_M1);
            case 0 -> mv.visitInsn(ICONST_0);
            case 1 -> mv.visitInsn(ICONST_1);
            case 2 -> mv.visitInsn(ICONST_2);
            case 3 -> mv.visitInsn(ICONST_3);
            case 4 -> mv.visitInsn(ICONST_4);
            case 5 -> mv.visitInsn(ICONST_5);
            default -> {
                if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                    mv.visitIntInsn(BIPUSH, value);
                } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                    mv.visitIntInsn(SIPUSH, value);
                } else {
                    mv.visitLdcInsn(value);
                }
            }
        }
    }

    // Load a local variable from its JVM slot onto the stack.
    private void loadLocal(MethodVisitor mv, LocalVar local) {
        switch (local.getType()) {
            case "INT", "BOOL" -> mv.visitVarInsn(ILOAD, local.getSlot());
            case "FLOAT" -> mv.visitVarInsn(FLOAD, local.getSlot());
            case "STRING" -> mv.visitVarInsn(ALOAD, local.getSlot());
            default -> mv.visitVarInsn(ALOAD, local.getSlot());
        }
    }

    // Store the top value of the JVM stack into a local variable slot.
    private void storeLocal(MethodVisitor mv, LocalVar local) {
        switch (local.getType()) {
            case "INT", "BOOL" -> mv.visitVarInsn(ISTORE, local.getSlot());
            case "FLOAT" -> mv.visitVarInsn(FSTORE, local.getSlot());
            case "STRING" -> mv.visitVarInsn(ASTORE, local.getSlot());
            default -> mv.visitVarInsn(ASTORE, local.getSlot());
        }
    }

    // Infer a simple expression type for code generation.
    // Semantic analysis already checked correctness, so this is only used to choose JVM instructions.
    private String inferExprType(ExprNode expr, CodeGenContext ctx) {

        if (expr instanceof LiteralNode literal) {
            return switch (literal.getKind()) {
                case "Integer" -> "INT";
                case "Float" -> "FLOAT";
                case "String" -> "STRING";
                case "Boolean" -> "BOOL";
                default -> throw new RuntimeException("Unknown literal kind: " + literal.getKind());
            };
        }

        if (expr instanceof IdentifierNode id) {
            LocalVar local = ctx.resolveLocal(id.getName());
            if (local == null) {
                throw new RuntimeException("Unknown local variable: " + id.getName());
            }
            return local.getType();
        }

        if (expr instanceof BinaryExprNode bin) {
            return switch (bin.getOperator()) {
                case "+", "-", "*", "/", "%" -> inferExprType(bin.getLeft(), ctx);
                case "==", "=/=", "<", ">", "<=", ">=", "&&", "||", "->" -> "BOOL";
                default -> throw new RuntimeException("Cannot infer binary operator type: " + bin.getOperator());
            };
        }

        if (expr instanceof CallNode call) {
            if (call.getCallee() instanceof IdentifierNode id) {
                String name = id.getName();

                return switch (name) {
                    case "println" -> "VOID";
                    case "read_INT" -> "INT";
                    case "read_FLOAT" -> "FLOAT";
                    case "read_STRING" -> "STRING";

                    default -> {
                        FunctionDefNode fn = functions.get(name);

                        if (fn == null) {
                            throw new RuntimeException("Unknown function: " + name);
                        }

                        if (fn.getReturnType() == null) {
                            yield "VOID";
                        }

                        yield normalizeType(fn.getReturnType());
                    }
                };
            }
        }

        if (expr instanceof IndexAccessNode index) {

            String targetType = inferExprType(index.getTarget(), ctx);

            if (targetType.startsWith("ARRAY[")) {
                return targetType.substring(6, targetType.length() - 1);
            }

            throw new RuntimeException(
                    "Cannot index non-array type: " + targetType
            );
        }

        throw new RuntimeException("Cannot infer expression type yet: " + expr.getClass().getSimpleName());
    }

    // Normalize alternate type names to the names used internally by code generation.
    private String normalizeType(String type) {
        if ("BOOLEAN".equals(type)) return "BOOL";
        return type;
    }

    // Build JVM descriptor for a function.
    private String methodDescriptor(FunctionDefNode fn) {
        StringBuilder sb = new StringBuilder();

        sb.append("(");

        for (ParamNode param : fn.getParams()) {
            sb.append(descriptor(param.getType()));
        }

        sb.append(")");

        if (fn.getReturnType() == null) {
            sb.append("V");
        } else {
            sb.append(descriptor(fn.getReturnType()));
        }

        return sb.toString();
    }

    // Infer descriptor for a function call.
    private String inferCallDescriptor(CallNode call, CodeGenContext ctx) {
        if (!(call.getCallee() instanceof IdentifierNode id)) {
            throw new RuntimeException("Only simple function calls are supported for now");
        }

        String name = id.getName();

        StringBuilder sb = new StringBuilder();

        sb.append("(");

        for (ExprNode arg : call.getArgs()) {
            sb.append(descriptor(inferExprType(arg, ctx)));
        }

        sb.append(")");

        // Built-ins.
        switch (name) {
            case "read_INT" -> {
                return "()I";
            }

            case "read_FLOAT" -> {
                return "()F";
            }

            case "read_STRING" -> {
                return "()Ljava/lang/String;";
            }

            case "println" -> {
                if (call.getArgs().isEmpty()) {
                    return "()V";
                }

                String argType = inferExprType(call.getArgs().get(0), ctx);
                return "(" + descriptor(argType) + ")V";
            }
        }

        FunctionDefNode fn = functions.get(name);

        if (fn == null) {
            throw new RuntimeException("Unknown function: " + name);
        }

        if (fn.getReturnType() == null) {
            sb.append("V");
        } else {
            sb.append(descriptor(fn.getReturnType()));
        }

        return sb.toString();
    }

    // Convert our language type names to JVM descriptors.
    private String descriptor(String type) {
        return switch (normalizeType(type)) {
            case "INT" -> "I";
            case "FLOAT" -> "F";
            case "BOOL" -> "Z";
            case "STRING" -> "Ljava/lang/String;";
            case "VOID" -> "V";
            default -> "L" + type + ";";
        };
    }

    private boolean isComparisonOperator(String op) {
        return "==".equals(op)
                || "=/=".equals(op)
                || "<".equals(op)
                || ">".equals(op)
                || "<=".equals(op)
                || ">=".equals(op)
                || "->".equals(op);
    }

    // Generate a condition as a real boolean value on stack: 0 or 1.
    private void generateComparisonExpr(MethodVisitor mv, BinaryExprNode bin, CodeGenContext ctx) {
        Label trueLabel = new Label();
        Label endLabel = new Label();

        generateConditionJump(mv, bin, ctx, true, trueLabel);

        mv.visitInsn(ICONST_0);
        mv.visitJumpInsn(GOTO, endLabel);

        mv.visitLabel(trueLabel);
        mv.visitInsn(ICONST_1);

        mv.visitLabel(endLabel);
    }

    // Generate && and || as boolean values on stack.
    private void generateLogicalExpr(MethodVisitor mv, BinaryExprNode bin, CodeGenContext ctx) {
        Label trueLabel = new Label();
        Label falseLabel = new Label();
        Label endLabel = new Label();

        generateConditionJump(mv, bin, ctx, true, trueLabel);

        mv.visitLabel(falseLabel);
        mv.visitInsn(ICONST_0);
        mv.visitJumpInsn(GOTO, endLabel);

        mv.visitLabel(trueLabel);
        mv.visitInsn(ICONST_1);

        mv.visitLabel(endLabel);
    }

    // Jump depending on whether condition should be true or false.
    private void generateConditionJump(MethodVisitor mv, ExprNode condition, CodeGenContext ctx, boolean jumpOnTrue, Label target) {
        if (condition instanceof BinaryExprNode bin) {
            String op = bin.getOperator();

            if ("&&".equals(op)) {
                if (jumpOnTrue) {
                    Label skip = new Label();
                    generateConditionJump(mv, bin.getLeft(), ctx, false, skip);
                    generateConditionJump(mv, bin.getRight(), ctx, true, target);
                    mv.visitLabel(skip);
                } else {
                    generateConditionJump(mv, bin.getLeft(), ctx, false, target);
                    generateConditionJump(mv, bin.getRight(), ctx, false, target);
                }
                return;
            }

            if ("||".equals(op)) {
                if (jumpOnTrue) {
                    generateConditionJump(mv, bin.getLeft(), ctx, true, target);
                    generateConditionJump(mv, bin.getRight(), ctx, true, target);
                } else {
                    Label skip = new Label();
                    generateConditionJump(mv, bin.getLeft(), ctx, true, skip);
                    generateConditionJump(mv, bin.getRight(), ctx, false, target);
                    mv.visitLabel(skip);
                }
                return;
            }

            if (isComparisonOperator(op)) {
                generateComparisonJump(mv, bin, ctx, jumpOnTrue, target);
                return;
            }
        }

        // General boolean expression: generate value, then compare with 0.
        generateExpr(mv, condition, ctx);
        mv.visitJumpInsn(jumpOnTrue ? IFNE : IFEQ, target);
    }

    private void generateComparisonJump(MethodVisitor mv, BinaryExprNode bin, CodeGenContext ctx, boolean jumpOnTrue, Label target) {
        String leftType = inferExprType(bin.getLeft(), ctx);
        String op = bin.getOperator();

        // Special case for the range operator. For now, we interpret "a -> b" as true, because the current parser represents it as a boolean condition, but does not connect it automatically to the loop variable yet.
        if ("->".equals(op)) {
            mv.visitInsn(ICONST_1);
            mv.visitJumpInsn(jumpOnTrue ? IFNE : IFEQ, target);
            return;
        }

        generateExpr(mv, bin.getLeft(), ctx);
        generateExpr(mv, bin.getRight(), ctx);

        if ("FLOAT".equals(leftType)) {
            mv.visitInsn(FCMPL);

            int opcode = switch (op) {
                case "==" -> jumpOnTrue ? IFEQ : IFNE;
                case "=/=" -> jumpOnTrue ? IFNE : IFEQ;
                case "<" -> jumpOnTrue ? IFLT : IFGE;
                case ">" -> jumpOnTrue ? IFGT : IFLE;
                case "<=" -> jumpOnTrue ? IFLE : IFGT;
                case ">=" -> jumpOnTrue ? IFGE : IFLT;
                default -> throw new RuntimeException("Unsupported float comparison: " + op);
            };

            mv.visitJumpInsn(opcode, target);
            return;
        }

        int opcode = switch (op) {
            case "==" -> jumpOnTrue ? IF_ICMPEQ : IF_ICMPNE;
            case "=/=" -> jumpOnTrue ? IF_ICMPNE : IF_ICMPEQ;
            case "<" -> jumpOnTrue ? IF_ICMPLT : IF_ICMPGE;
            case ">" -> jumpOnTrue ? IF_ICMPGT : IF_ICMPLE;
            case "<=" -> jumpOnTrue ? IF_ICMPLE : IF_ICMPGT;
            case ">=" -> jumpOnTrue ? IF_ICMPGE : IF_ICMPLT;
            default -> throw new RuntimeException("Unsupported int comparison: " + op);
        };

        mv.visitJumpInsn(opcode, target);
    }

    private void generateForConditionJump(MethodVisitor mv, ForNode forNode, CodeGenContext ctx, Label endLabel) {
        ExprNode condition = forNode.getCondition();

        // Special handling for: for (i; start -> end; i+1)
        if (condition instanceof BinaryExprNode bin && "->".equals(bin.getOperator())) {
            LocalVar loopVar = getForLoopVariable(forNode, ctx);

            generateExpr(mv, bin.getRight(), ctx); // upper bound
            loadLocal(mv, loopVar); // current i

            // if upperBound < i, end loop
            mv.visitJumpInsn(IF_ICMPLT, endLabel);
            return;
        }

        generateConditionJump(mv, condition, ctx, false, endLabel);
    }

    private void generateForUpdate(MethodVisitor mv, ForNode forNode, ExprNode updateExpr, CodeGenContext ctx) {
        LocalVar loopVar = getForLoopVariable(forNode, ctx);

        // Special handling for update like: i + 1
        if (updateExpr instanceof BinaryExprNode bin
                && bin.getLeft() instanceof IdentifierNode id
                && id.getName().equals(loopVar.getName())
                && "+".equals(bin.getOperator())) {

            // Generate i + right.
            generateExpr(mv, bin, ctx);

            // Store result back into i.
            storeLocal(mv, loopVar);
            return;
        }

        // Generic expression update.
        generateExpr(mv, updateExpr, ctx);
        String updateType = inferExprType(updateExpr, ctx);
        if (!"VOID".equals(updateType)) {
            mv.visitInsn(POP);
        }
    }

    private LocalVar getForLoopVariable(ForNode forNode, CodeGenContext ctx) {
        if (forNode.getInit() instanceof IdentifierNode id) {
            LocalVar local = ctx.resolveLocal(id.getName());
            if (local == null) {
                throw new RuntimeException("Unknown for-loop variable: " + id.getName());
            }
            return local;
        }

        if (forNode.getInit() instanceof VarDeclNode varDecl) {
            LocalVar local = ctx.resolveLocal(varDecl.getName());
            if (local == null) {
                throw new RuntimeException("Unknown for-loop variable: " + varDecl.getName());
            }
            return local;
        }

        throw new RuntimeException("For loop needs an identifier or variable declaration as init");
    }
}