package compiler.Generation;

import java.io.FileOutputStream;
import java.io.IOException;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.FADD;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FDIV;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FMUL;
import static org.objectweb.asm.Opcodes.FREM;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.FSUB;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.ICONST_M1;
import static org.objectweb.asm.Opcodes.IDIV;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IMUL;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IREM;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.ISUB;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SIPUSH;
import static org.objectweb.asm.Opcodes.V17;

import compiler.Parser.ASTNode;
import compiler.Parser.AssignmentNode;
import compiler.Parser.BinaryExprNode;
import compiler.Parser.BlockNode;
import compiler.Parser.CallNode;
import compiler.Parser.ExprNode;
import compiler.Parser.ExprStatementNode;
import compiler.Parser.FunctionDefNode;
import compiler.Parser.IdentifierNode;
import compiler.Parser.LiteralNode;
import compiler.Parser.ProgramNode;
import compiler.Parser.StatementNode;
import compiler.Parser.VarDeclNode;

public class CodeGenerator {

    private final String className;

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

        // Search all top-level nodes. If we find a function named "main", we generate the JVM main method.
        for (ASTNode node : program.getElements()) {
            if (node instanceof FunctionDefNode fn && "main".equals(fn.getName())) {
                generateMain(cw, fn);
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

        // Assignment, for example: x = x + 1;
        if (stmt instanceof AssignmentNode assignment) {
            if (!(assignment.getTarget() instanceof IdentifierNode id)) {
                throw new RuntimeException("Step 5 only supports assignment to identifiers");
            }

            LocalVar local = ctx.resolveLocal(id.getName());
            if (local == null) {
                throw new RuntimeException("Unknown local variable: " + id.getName());
            }

            // Generate the right-hand side value.
            generateExpr(mv, assignment.getValue(), ctx);

            // Store it into the variable target.
            storeLocal(mv, local);
            return;
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

        throw new RuntimeException(
                "Step 5 code generation not implemented yet for expression: "
                        + expr.getClass().getSimpleName()
        );
    }

    // Generate a binary expression.
    private void generateBinaryExpr(MethodVisitor mv, BinaryExprNode bin, CodeGenContext ctx) {
        String type = inferExprType(bin.getLeft(), ctx);
        String op = bin.getOperator();

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

        throw new RuntimeException(
                "Unknown function in Step 5 code generation: " + name
        );
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
                default -> throw new RuntimeException("Step 5 cannot infer binary operator type: " + bin.getOperator());
            };
        }

        if (expr instanceof CallNode call) {
            if (call.getCallee() instanceof IdentifierNode id) {
                String name = id.getName();
                if ("println".equals(name)) {
                    return "VOID";
                }
            }
        }

        throw new RuntimeException("Cannot infer expression type yet: " + expr.getClass().getSimpleName());
    }

    // Normalize alternate type names to the names used internally by code generation.
    private String normalizeType(String type) {
        if ("BOOLEAN".equals(type)) return "BOOL";
        return type;
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
}