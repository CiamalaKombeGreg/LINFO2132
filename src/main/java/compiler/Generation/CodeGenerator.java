package compiler.Generation;

import java.io.File;
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
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SIPUSH;
import static org.objectweb.asm.Opcodes.T_INT;
import static org.objectweb.asm.Opcodes.V17;

import compiler.Parser.ASTNode;
import compiler.Parser.AssignmentNode;
import compiler.Parser.BinaryExprNode;
import compiler.Parser.BlockNode;
import compiler.Parser.CallNode;
import compiler.Parser.CollDeclNode;
import compiler.Parser.ExprNode;
import compiler.Parser.ExprStatementNode;
import compiler.Parser.FieldNode;
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

    // Generates the main class and output files.
    public void generate(ProgramNode program, String outputPath) throws IOException {

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        cw.visit(
                V17,
                ACC_PUBLIC | ACC_SUPER,
                className,
                null,
                "java/lang/Object",
                null
        );

        generateConstructor(cw);

        for (ASTNode node : program.getElements()) {
            if (node instanceof CollDeclNode coll) {
                generateCollectionClass(coll, outputPath);
            }
        }

        for (ASTNode node : program.getElements()) {
            if (node instanceof FunctionDefNode fn) {
                functions.put(fn.getName(), fn);
            }
        }

        for (ASTNode node : program.getElements()) {
            if (node instanceof FunctionDefNode fn) {
                generateFunction(cw, fn);
            }
        }

        cw.visitEnd();

        try (FileOutputStream out = new FileOutputStream(outputPath)) {
            out.write(cw.toByteArray());
        }
    }

    // Adds the default Java constructor.
    private void generateConstructor(ClassWriter cw) {

        MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "<init>",
                "()V",
                null,
                null
        );

        mv.visitCode();

        mv.visitVarInsn(ALOAD, 0);

        mv.visitMethodInsn(
                INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V",
                false
        );

        mv.visitInsn(RETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    // Emits the JVM entry point.
    private void generateMain(ClassWriter cw, FunctionDefNode fn) {

        MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC | ACC_STATIC,
                "main",
                "([Ljava/lang/String;)V",
                null,
                null
        );

        mv.visitCode();

        CodeGenContext ctx = new CodeGenContext(1);

        generateBlock(mv, fn.getBody(), ctx);

        mv.visitInsn(RETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    // Emits a static method for a language function.
    private void generateFunction(ClassWriter cw, FunctionDefNode fn) {

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

        CodeGenContext ctx = new CodeGenContext(0);

        for (ParamNode param : fn.getParams()) {
            ctx.declareLocal(
                    param.getName(),
                    normalizeType(param.getType())
            );
        }

        generateBlock(mv, fn.getBody(), ctx);

        if (fn.getReturnType() == null || "VOID".equals(fn.getReturnType())) {
            mv.visitInsn(RETURN);
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    // Emits every statement in a block.
    private void generateBlock(MethodVisitor mv, BlockNode block, CodeGenContext ctx) {
        for (StatementNode stmt : block.getStatements()) {
            generateStatement(mv, stmt, ctx);
        }
    }

    // Emits bytecode for one statement.
    private void generateStatement(MethodVisitor mv, StatementNode stmt, CodeGenContext ctx) {

        if (stmt instanceof VarDeclNode varDecl) {
            LocalVar local = ctx.declareLocal(varDecl.getName(), normalizeType(varDecl.getType()));

            if (varDecl.getInitializer() != null) {
                generateExpr(mv, varDecl.getInitializer(), ctx);
            } else {
                generateDefaultValue(mv, local.getType());
            }

            storeLocal(mv, local);
            return;
        }

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

        if (stmt instanceof ExprStatementNode exprStmt) {
            generateExpr(mv, exprStmt.getExpr(), ctx);

            String type = inferExprType(exprStmt.getExpr(), ctx);
            if (!"VOID".equals(type)) {
                mv.visitInsn(POP);
            }
            return;
        }

        if (stmt instanceof IfNode ifNode) {
            Label elseLabel = new Label();
            Label endLabel = new Label();

            generateConditionJump(mv, ifNode.getCondition(), ctx, false, elseLabel);

            generateStatement(mv, ifNode.getThenBranch(), ctx);

            mv.visitJumpInsn(GOTO, endLabel);

            mv.visitLabel(elseLabel);
            if (ifNode.getElseBranch() != null) {
                generateStatement(mv, ifNode.getElseBranch(), ctx);
            }

            mv.visitLabel(endLabel);
            return;
        }

        if (stmt instanceof WhileNode whileNode) {
            Label startLabel = new Label();
            Label endLabel = new Label();

            mv.visitLabel(startLabel);

            generateConditionJump(mv, whileNode.getCondition(), ctx, false, endLabel);

            generateStatement(mv, whileNode.getBody(), ctx);

            mv.visitJumpInsn(GOTO, startLabel);

            mv.visitLabel(endLabel);
            return;
        }

        if (stmt instanceof BlockNode block) {
            generateBlock(mv, block, ctx);
            return;
        }

        if (stmt instanceof ForNode forNode) {
            Label startLabel = new Label();
            Label endLabel = new Label();

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

            mv.visitLabel(startLabel);

            if (forNode.getCondition() != null) {
                generateForConditionJump(mv, forNode, ctx, endLabel);
            }

            generateStatement(mv, forNode.getBody(), ctx);

            if (forNode.getUpdate() instanceof StatementNode updateStmt) {
                generateStatement(mv, updateStmt, ctx);
            } else if (forNode.getUpdate() instanceof ExprNode updateExpr) {
                generateForUpdate(mv, forNode, updateExpr, ctx);
            }

            mv.visitJumpInsn(GOTO, startLabel);

            mv.visitLabel(endLabel);
            return;
        }

        if (stmt instanceof ReturnNode ret) {

            if (ret.getExpr() == null) {
                mv.visitInsn(RETURN);
                return;
            }

            generateExpr(mv, ret.getExpr(), ctx);

            String type = inferExprType(ret.getExpr(), ctx);

            switch (type) {
                case "INT", "BOOL" -> mv.visitInsn(IRETURN);
                case "FLOAT" -> mv.visitInsn(FRETURN);
                default -> mv.visitInsn(ARETURN);
            }

            return;
        }

        if (stmt instanceof AssignmentNode assign) {

            if (assign.getTarget() instanceof IndexAccessNode index) {

                generateExpr(mv, index.getTarget(), ctx);

                generateExpr(mv, index.getIndex(), ctx);

                generateExpr(mv, assign.getValue(), ctx);

                String targetType = inferExprType(index.getTarget(), ctx);

                if ("ARRAY[INT]".equals(targetType)) {
                    mv.visitInsn(IASTORE);
                    return;
                }

                mv.visitInsn(AASTORE);
                return;
            }

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

    // Emits bytecode for one expression.
    private void generateExpr(MethodVisitor mv, ExprNode expr, CodeGenContext ctx) {

        if (expr instanceof LiteralNode literal) {
            generateLiteral(mv, literal);
            return;
        }

        if (expr instanceof IdentifierNode id) {
            LocalVar local = ctx.resolveLocal(id.getName());
            if (local == null) {
                throw new RuntimeException("Unknown local variable: " + id.getName());
            }

            loadLocal(mv, local);
            return;
        }

        if (expr instanceof BinaryExprNode bin) {
            generateBinaryExpr(mv, bin, ctx);
            return;
        }

        if (expr instanceof CallNode call) {
            generateCall(mv, call, ctx);
            return;
        }

        if (expr instanceof IndexAccessNode index) {

            generateExpr(mv, index.getTarget(), ctx);

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

    // Emits arithmetic, comparison, and logical expressions.
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

        generateExpr(mv, bin.getLeft(), ctx);

        generateExpr(mv, bin.getRight(), ctx);

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

    // Emits built-ins, constructors, and function calls.
    private void generateCall(MethodVisitor mv, CallNode call, CodeGenContext ctx) {

        if (!(call.getCallee() instanceof IdentifierNode id)) {
            throw new RuntimeException("Only simple function calls are supported for now");
        }

        String name = id.getName();

        if ("INT ARRAY".equals(name)) {

            if (call.getArgs().size() != 1) {
                throw new RuntimeException("INT ARRAY expects one size argument");
            }

            generateExpr(mv, call.getArgs().get(0), ctx);

            mv.visitIntInsn(NEWARRAY, T_INT);

            return;
        }

        if ("println".equals(name) || "print".equals(name) || "print_INT".equals(name) || "print_FLOAT".equals(name)) {
            mv.visitFieldInsn(
                    GETSTATIC,
                    "java/lang/System",
                    "out",
                    "Ljava/io/PrintStream;"
            );

            boolean isPrintln = "println".equals(name);

            if (call.getArgs().isEmpty()) {
                mv.visitMethodInsn(
                        INVOKEVIRTUAL,
                        "java/io/PrintStream",
                        isPrintln ? "println" : "print",
                        "()V",
                        false
                );
                return;
            }

            if (call.getArgs().size() == 1) {
                ExprNode arg = call.getArgs().get(0);
                String argType = inferExprType(arg, ctx);

                generateExpr(mv, arg, ctx);

                mv.visitMethodInsn(
                        INVOKEVIRTUAL,
                        "java/io/PrintStream",
                        isPrintln ? "println" : "print",
                        "(" + descriptor(argType) + ")V",
                        false
                );
                return;
            }

            throw new RuntimeException(name + " accepts zero or one argument");
        }

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

    // Writes a JVM class for a collection.
    private void generateCollectionClass(CollDeclNode coll, String outputPath) throws IOException {

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        cw.visit(
                V17,
                ACC_PUBLIC | ACC_SUPER,
                coll.getName(),
                null,
                "java/lang/Object",
                null
        );

        for (FieldNode field : coll.getFields()) {

            cw.visitField(
                    ACC_PUBLIC,
                    field.getName(),
                    descriptor(field.getType()),
                    null,
                    null
            ).visitEnd();
        }

        generateCollectionConstructor(cw, coll);

        cw.visitEnd();

        File outFile = new File(outputPath).getParentFile();

        if (outFile == null) {
            outFile = new File(".");
        }

        try (FileOutputStream out = new FileOutputStream(
                new File(outFile, coll.getName() + ".class"))) {

            out.write(cw.toByteArray());
        }
    }

    // Pushes literal values on the stack.
    private void generateLiteral(MethodVisitor mv, LiteralNode literal) {

        switch (literal.getKind()) {
            case "Integer" -> pushInt(mv, Integer.parseInt(literal.getValue()));

            case "Float" -> mv.visitLdcInsn(Float.parseFloat(literal.getValue()));

            case "String" -> mv.visitLdcInsn(literal.getValue());

            case "Boolean" -> mv.visitInsn(Boolean.parseBoolean(literal.getValue()) ? ICONST_1 : ICONST_0);

            default -> throw new RuntimeException("Unknown literal kind: " + literal.getKind());
        }
    }

    // Pushes default values for variables.
    private void generateDefaultValue(MethodVisitor mv, String type) {
        switch (type) {
            case "INT", "BOOL" -> mv.visitInsn(ICONST_0);
            case "FLOAT" -> mv.visitInsn(FCONST_0);
            case "STRING" -> mv.visitInsn(ACONST_NULL);
            default -> mv.visitInsn(ACONST_NULL);
        }
    }

    // Pushes an integer with a compact instruction.
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

    // Reads a local variable.
    private void loadLocal(MethodVisitor mv, LocalVar local) {
        switch (local.getType()) {
            case "INT", "BOOL" -> mv.visitVarInsn(ILOAD, local.getSlot());
            case "FLOAT" -> mv.visitVarInsn(FLOAD, local.getSlot());
            case "STRING" -> mv.visitVarInsn(ALOAD, local.getSlot());
            default -> mv.visitVarInsn(ALOAD, local.getSlot());
        }
    }

    // Writes a local variable.
    private void storeLocal(MethodVisitor mv, LocalVar local) {
        switch (local.getType()) {
            case "INT", "BOOL" -> mv.visitVarInsn(ISTORE, local.getSlot());
            case "FLOAT" -> mv.visitVarInsn(FSTORE, local.getSlot());
            case "STRING" -> mv.visitVarInsn(ASTORE, local.getSlot());
            default -> mv.visitVarInsn(ASTORE, local.getSlot());
        }
    }

    // Finds the JVM type of an expression.
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
                    case "println", "print", "print_INT", "print_FLOAT" -> "VOID";
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

    // Normalizes type aliases.
    private String normalizeType(String type) {
        if ("BOOLEAN".equals(type)) return "BOOL";
        return type;
    }

    // Builds a JVM descriptor for a function.
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

    // Builds a JVM descriptor for a call.
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

            case "print_INT" -> {
                return "(I)V";
            }

            case "print_FLOAT" -> {
                return "(F)V";
            }

            case "print", "println" -> {
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

    // Converts language types to JVM descriptors.
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

    // Checks comparison operators.
    private boolean isComparisonOperator(String op) {
        return "==".equals(op)
                || "=/=".equals(op)
                || "<".equals(op)
                || ">".equals(op)
                || "<=".equals(op)
                || ">=".equals(op)
                || "->".equals(op);
    }

    // Emits a comparison as 0 or 1.
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

    // Emits logical operators as 0 or 1.
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

    // Emits a branch based on a condition.
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

        generateExpr(mv, condition, ctx);
        mv.visitJumpInsn(jumpOnTrue ? IFNE : IFEQ, target);
    }

    // Emits comparison jump instructions.
    private void generateComparisonJump(MethodVisitor mv, BinaryExprNode bin, CodeGenContext ctx, boolean jumpOnTrue, Label target) {
        String leftType = inferExprType(bin.getLeft(), ctx);
        String op = bin.getOperator();

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

    // Emits the condition branch for for loops.
    private void generateForConditionJump(MethodVisitor mv, ForNode forNode, CodeGenContext ctx, Label endLabel) {
        ExprNode condition = forNode.getCondition();

        if (condition instanceof BinaryExprNode bin && "->".equals(bin.getOperator())) {
            LocalVar loopVar = getForLoopVariable(forNode, ctx);

            generateExpr(mv, bin.getRight(), ctx);
            loadLocal(mv, loopVar);

            mv.visitJumpInsn(IF_ICMPLT, endLabel);
            return;
        }

        generateConditionJump(mv, condition, ctx, false, endLabel);
    }

    // Emits the update part of a for loop.
    private void generateForUpdate(MethodVisitor mv, ForNode forNode, ExprNode updateExpr, CodeGenContext ctx) {
        LocalVar loopVar = getForLoopVariable(forNode, ctx);

        if (updateExpr instanceof BinaryExprNode bin
                && bin.getLeft() instanceof IdentifierNode id
                && id.getName().equals(loopVar.getName())
                && "+".equals(bin.getOperator())) {

            generateExpr(mv, bin, ctx);

            storeLocal(mv, loopVar);
            return;
        }

        generateExpr(mv, updateExpr, ctx);
        String updateType = inferExprType(updateExpr, ctx);
        if (!"VOID".equals(updateType)) {
            mv.visitInsn(POP);
        }
    }

    // Resolves the variable controlled by a for loop.
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

    // Emits a constructor for a collection class.
    private void generateCollectionConstructor(ClassWriter cw, CollDeclNode coll) {

        StringBuilder desc = new StringBuilder();

        desc.append("(");

        for (FieldNode field : coll.getFields()) {
            desc.append(descriptor(field.getType()));
        }

        desc.append(")V");

        MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "<init>",
                desc.toString(),
                null,
                null
        );

        mv.visitCode();

        mv.visitVarInsn(ALOAD, 0);

        mv.visitMethodInsn(
                INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V",
                false
        );

        int slot = 1;

        for (FieldNode field : coll.getFields()) {

            mv.visitVarInsn(ALOAD, 0);

            switch (normalizeType(field.getType())) {
                case "INT", "BOOL" -> mv.visitVarInsn(ILOAD, slot);
                case "FLOAT" -> mv.visitVarInsn(FLOAD, slot);
                default -> mv.visitVarInsn(ALOAD, slot);
            }

            mv.visitFieldInsn(
                    PUTFIELD,
                    coll.getName(),
                    field.getName(),
                    descriptor(field.getType())
            );

            slot++;
        }

        mv.visitInsn(RETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
