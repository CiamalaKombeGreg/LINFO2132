import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import compiler.Generation.CodeGenerator;
import compiler.Lexer.Lexer;
import compiler.Parser.ASTNode;
import compiler.Parser.Parser;
import compiler.Parser.ProgramNode;
import compiler.Semantic.SemanticAnalyzer;

public class TestGeneration {

    private static final Path PROJECT_ROOT = Path.of("tests").toAbsolutePath();

    private ProgramNode parseProgram(Path source) throws IOException {
        try (Reader reader = new FileReader(source.toFile())) {
            Lexer lexer = new Lexer(reader);
            Parser parser = new Parser(lexer);
            ASTNode ast = parser.getAST();

            if (!(ast instanceof ProgramNode program)) {
                throw new RuntimeException("AST root is not a ProgramNode");
            }

            return program;
        }
    }

    private Path generateClass(String sourceFileName, Path outputDir) throws Exception {
        Path source = PROJECT_ROOT.resolve(sourceFileName);
        Path output = outputDir.resolve("Test.class");

        ProgramNode program = parseProgram(source);

        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.analyze(program);

        CodeGenerator generator = new CodeGenerator("Test");
        generator.generate(program, output.toString());

        assertTrue("Expected generated class file: " + output, Files.exists(output));
        return output;
    }

    private String runGeneratedClass(Path outputDir, String stdin) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("java", "-cp", outputDir.toString(), "Test");
        pb.redirectErrorStream(true);

        Process process = pb.start();

        if (stdin != null && !stdin.isEmpty()) {
            process.getOutputStream().write(stdin.getBytes(StandardCharsets.UTF_8));
        }
        process.getOutputStream().close();

        String output = readAll(process.getInputStream());
        int exit = process.waitFor();

        assertEquals("Generated class exited with non-zero code. Output:\n" + output, 0, exit);

        return normalize(output);
    }

    private String compileAndRun(String sourceFileName, String stdin) throws Exception {
        Path outputDir = Files.createTempDirectory("codegen-test-");

        generateClass(sourceFileName, outputDir);

        return runGeneratedClass(outputDir, stdin);
    }

    private String compileAndRunSource(String sourceCode, String className) throws Exception {
        Path outputDir = Files.createTempDirectory("codegen-inline-test-");
        Path sourceFile = outputDir.resolve(className + ".lang");
        Path classFile = outputDir.resolve(className + ".class");

        Files.writeString(sourceFile, sourceCode);

        ProgramNode program = parseProgram(sourceFile);

        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.analyze(program);

        CodeGenerator generator = new CodeGenerator(className);
        generator.generate(program, classFile.toString());

        ProcessBuilder pb = new ProcessBuilder("java", "-cp", outputDir.toString(), className);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        String output = readAll(process.getInputStream());
        int exit = process.waitFor();

        assertEquals("Generated class exited with non-zero code. Output:\n" + output, 0, exit);

        return normalize(output);
    }

    private void expectOutput(String sourceFileName, String expectedOutput) throws Exception {
        assertEquals(normalize(expectedOutput), compileAndRun(sourceFileName, ""));
    }

    private void expectOutputWithInput(String sourceFileName, String input, String expectedOutput) throws Exception {
        assertEquals(normalize(expectedOutput), compileAndRun(sourceFileName, input));
    }

    private void expectGenerationError(String sourceFileName) throws Exception {
        try {
            Path outputDir = Files.createTempDirectory("codegen-error-test-");
            Path source = PROJECT_ROOT.resolve(sourceFileName);
            ProgramNode program = parseProgram(source);

            CodeGenerator generator = new CodeGenerator("Test");
            generator.generate(program, outputDir.resolve("Test.class").toString());
        } catch (Exception e) {
            return;
        }

        throw new AssertionError("Expected generation error for " + sourceFileName);
    }

    private String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        in.transferTo(out);
        return out.toString(StandardCharsets.UTF_8);
    }

    private String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n").trim();
    }

    @Test
    public void test01PrintHello() throws Exception {
        expectOutput("test1.lang", "hello");
    }

    @Test
    public void test02LocalVariablesArithmeticAssignmentAndPrintln() throws Exception {
        expectOutput("test2.lang", """
                11
                16.0
                hello
                true
                10
                """);
    }

    @Test
    public void test03IfElseAndWhile() throws Exception {
        expectOutput("test3.lang", """
                x smaller
                5
                6
                7
                """);
    }

    @Test
    public void test04ForLoopRange() throws Exception {
        expectOutput("test4.lang", """
                1
                2
                3
                4
                5
                """);
    }

    @Test
    public void test05FunctionReturningInt() throws Exception {
        expectOutput("test5.lang", "25");
    }

    @Test
    public void test06FunctionsReturningFloatAndString() throws Exception {
        expectOutput("test6.lang", """
                5.0
                hello
                """);
    }

    @Test
    public void test07IntArrayCreationAssignmentAndAccess() throws Exception {
        expectOutput("test7.lang", """
                10
                20
                """);
    }

    @Test
    public void test08CollectionClassGeneration() throws Exception {
        Path outputDir = Files.createTempDirectory("codegen-coll-test-");

        generateClass("test8.lang", outputDir);

        assertTrue(Files.exists(outputDir.resolve("Test.class")));
        assertTrue(Files.exists(outputDir.resolve("Point.class")));

        assertEquals("ok", runGeneratedClass(outputDir, ""));
    }

    @Test
    public void test09CollectionConstructorFieldAccessAndFieldAssignment() throws Exception {
        expectOutput("test9.lang", """
                3
                42
                """);
    }

    @Test
    public void test10aGlobalVariables() throws Exception {
        expectOutput("test10a.lang", """
                5
                7
                """);
    }

    @Test
    public void test10bReadIntBuiltIn() throws Exception {
        expectOutputWithInput("test10b.lang", "42\n", "42");
    }

    @Test
    public void testProfessorCorrectTest() throws Exception {
        expectOutputWithInput("test.lang", "3\n", "9");
    }

    @Test
    public void testE01GenerationError() throws Exception {
        expectGenerationError("testE1.lang");
    }

    @Test
    public void testE02GenerationError() throws Exception {
        expectGenerationError("testE2.lang");
    }

    @Test
    public void testE03GenerationError() throws Exception {
        expectGenerationError("testE3.lang");
    }

    @Test
    public void testE04GenerationError() throws Exception {
        expectGenerationError("testE4.lang");
    }

    @Test
    public void testE05GenerationError() throws Exception {
        expectGenerationError("testE5.lang");
    }

    @Test
    public void testE06GenerationError() throws Exception {
        expectGenerationError("testE6.lang");
    }

    @Test
    public void testE07GenerationError() throws Exception {
        expectGenerationError("testE7.lang");
    }

    @Test
    public void testE08GenerationError() throws Exception {
        expectGenerationError("testE8.lang");
    }

    @Test
    public void testE09GenerationError() throws Exception {
        expectGenerationError("testE9.lang");
    }

    @Test
    public void testE10GenerationError() throws Exception {
        expectGenerationError("testE10.lang");
    }

    @Test
    public void testVoidFunctionWorks() throws Exception {
        String source = """
                def sayHello() {
                    println("hello");
                }

                def main() {
                    sayHello();
                    println("done");
                }
                """;

        String output = compileAndRunSource(source, "VoidFunctionTest");

        assertEquals("hello\ndone", output);
    }

    @Test
    public void testSameVariableNameInDifferentFunctions() throws Exception {
        String source = """
                def helper() {
                    INT x = 5;
                    println(x);
                }

                def main() {
                    INT x = 10;
                    println(x);
                    helper();
                }
                """;

        String output = compileAndRunSource(source, "SameLocalNameTest");

        assertEquals("10\n5", output);
    }
}
