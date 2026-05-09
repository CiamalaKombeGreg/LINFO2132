package compiler.Generation;

import java.io.FileOutputStream;
import java.io.IOException;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V17;

import compiler.Parser.ASTNode;
import compiler.Parser.BlockNode;
import compiler.Parser.CallNode;
import compiler.Parser.ExprNode;
import compiler.Parser.ExprStatementNode;
import compiler.Parser.FunctionDefNode;
import compiler.Parser.IdentifierNode;
import compiler.Parser.LiteralNode;
import compiler.Parser.ProgramNode;
import compiler.Parser.StatementNode;

public class CodeGenerator {

    private final String className;

    public CodeGenerator(String className) {
        this.className = className;
    }

    public void generate(ProgramNode program, String outputPath) throws IOException {

        // ASM helper that builds the bytecode of the class.
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS); // COMPUTE_FRAMES and COMPUTE_MAXS let ASM calculate stack sizes automatically.

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

        // Search all top-level nodes. If we find a function named "main", we generate the main method.
        for (ASTNode node : program.getElements()) {
            if (node instanceof FunctionDefNode fn && "main".equals(fn.getName())) {
                generateMain(cw, fn);
            }
        }

        cw.visitEnd();

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

        // Generate all statements inside the block.
        generateBlock(mv, fn.getBody());

        // End main function.
        mv.visitInsn(RETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    // Generate all statements of a block one after another.
    private void generateBlock(MethodVisitor mv, BlockNode block) {
        for (StatementNode stmt : block.getStatements()) {
            generateStatement(mv, stmt);
        }
    }

    // Generate one statement.
    private void generateStatement(MethodVisitor mv, StatementNode stmt) {

        if (stmt instanceof ExprStatementNode exprStmt) {
            generateExpr(mv, exprStmt.getExpr());
            return;
        }

        throw new RuntimeException(
                "Code generation not implemented yet for statement: "
                + stmt.getClass().getSimpleName()
        );
    }

    // Generate expressions.
    private void generateExpr(MethodVisitor mv, ExprNode expr) {

        // Function call.
        if (expr instanceof CallNode call) {
            generateCall(mv, call);
            return;
        }

        // Literal value.
        if (expr instanceof LiteralNode literal) {
            generateLiteral(mv, literal);
            return;
        }

        throw new RuntimeException(
                "Code generation not implemented yet for expression: "
                + expr.getClass().getSimpleName()
        );
    }

    // Generate a function call.
    private void generateCall(MethodVisitor mv, CallNode call) {

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
            if (call.getArgs().size() == 0) {

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

                // Generate the argument value first.
                generateExpr(mv, arg);

                // For now, only string literals are supported.
                if (arg instanceof LiteralNode literal
                        && "String".equals(literal.getKind())) {

                    mv.visitMethodInsn(
                            INVOKEVIRTUAL,
                            "java/io/PrintStream",
                            "println",
                            "(Ljava/lang/String;)V",
                            false
                    );

                    return;
                }

                throw new RuntimeException(
                        "For now, println only supports string literals"
                );
            }

            throw new RuntimeException(
                    "println accepts zero or one argument"
            );
        }

        throw new RuntimeException(
                "Unknown function in code generation: " + name
        );
    }

    // Generate literal values.
    private void generateLiteral(MethodVisitor mv, LiteralNode literal) {

        // Push a string constant onto the JVM stack.
        if ("String".equals(literal.getKind())) {
            mv.visitLdcInsn(literal.getValue());
            return;
        }

        throw new RuntimeException(
                "For now, only string literals are supported"
        );
    }
}