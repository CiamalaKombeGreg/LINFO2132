import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;

import compiler.Lexer.Lexer;
import compiler.Parser.ASTNode;
import compiler.Parser.Parser;

public class TestParser {

    private ASTNode parse(String input) throws IOException {
        Lexer lexer = new Lexer(new StringReader(input));
        Parser parser = new Parser(lexer);
        return parser.getAST();
    }

    private void expectParses(String input) throws IOException {
        ASTNode ast = parse(input);
        assertNotNull(ast);
    }

    @Test
    public void testEmptyProgram() throws IOException {
        expectParses("");
    }

    @Test
    public void testSimpleVarDecl() throws IOException {
        expectParses("INT x;");
    }

    @Test
    public void testFinalVarDecl() throws IOException {
        expectParses("final INT x;");
    }

    @Test
    public void testVarDeclWithInitializer() throws IOException {
        expectParses("INT x = 42;");
    }

    @Test
    public void testArrayVarDecl() throws IOException {
        expectParses("ARRAY[INT] xs;");
    }

    @Test
    public void testNestedArrayVarDecl() throws IOException {
        expectParses("ARRAY[ARRAY[INT]] matrix;");
    }

    @Test
    public void testCollectionTypeVarDecl() throws IOException {
        expectParses("""
                coll Person {
                    STRING name;
                }
                Person p;
                """);
    }

    @Test
    public void testBlock() throws IOException {
        expectParses("""
                {
                    INT x;
                    x;
                }
                """);
    }

    @Test
    public void testNestedBlock() throws IOException {
        expectParses("""
                {
                    INT x;
                    {
                        INT y;
                    }
                }
                """);
    }

    @Test
    public void testIdentifierAssignment() throws IOException {
        expectParses("""
                INT x;
                x = 5;
                """);
    }

    @Test
    public void testIndexAssignment() throws IOException {
        expectParses("""
                ARRAY[INT] xs;
                xs[0] = 10;
                """);
    }

    @Test
    public void testFieldAssignment() throws IOException {
        expectParses("""
                coll Person {
                    STRING name;
                }
                Person p;
                p.name = "Alice";
                """);
    }

    @Test
    public void testReturnWithoutValue() throws IOException {
        expectParses("""
                def f() {
                    return;
                }
                """);
    }

    @Test
    public void testReturnWithValue() throws IOException {
        expectParses("""
                def f() {
                    return 123;
                }
                """);
    }

    @Test
    public void testIfWithoutElse() throws IOException {
        expectParses("""
                if (true) {
                    INT x;
                }
                """);
    }

    @Test
    public void testIfWithElse() throws IOException {
        expectParses("""
                if (true) {
                    INT x;
                } else {
                    INT y;
                }
                """);
    }

    @Test
    public void testWhileLoop() throws IOException {
        expectParses("""
                while (true) {
                    return;
                }
                """);
    }

    @Test
    public void testForLoopEmptyParts() throws IOException {
        expectParses("""
                for (;;) {
                    return;
                }
                """);
    }

    @Test
    public void testForLoopWithVarDeclInit() throws IOException {
        expectParses("""
                for (INT i = 0; i < 10; i = i + 1) {
                    return;
                }
                """);
    }

    @Test
    public void testForLoopWithAssignmentInit() throws IOException {
        expectParses("""
                INT i;
                for (i = 0; i < 10; i = i + 1) {
                    return;
                }
                """);
    }

    @Test
    public void testFunctionWithoutParameters() throws IOException {
        expectParses("""
                def main() {
                    return;
                }
                """);
    }

    @Test
    public void testFunctionWithOneParameter() throws IOException {
        expectParses("""
                def inc(INT x) {
                    return x;
                }
                """);
    }

    @Test
    public void testFunctionWithMultipleParameters() throws IOException {
        expectParses("""
                def sum(INT a, FLOAT b, STRING c) {
                    return a;
                }
                """);
    }

    @Test
    public void testCollectionDeclaration() throws IOException {
        expectParses("""
                coll Person {
                    STRING name;
                    INT age;
                }
                """);
    }

    @Test
    public void testCollectionDeclarationWithArrayField() throws IOException {
        expectParses("""
                coll Group {
                    ARRAY[INT] values;
                }
                """);
    }

    @Test
    public void testIntLiteralExprStatement() throws IOException {
        expectParses("42;");
    }

    @Test
    public void testFloatLiteralExprStatement() throws IOException {
        expectParses("3.14;");
    }

    @Test
    public void testStringLiteralExprStatement() throws IOException {
        expectParses("\"hello\";");
    }

    @Test
    public void testBooleanLiteralExprStatement() throws IOException {
        expectParses("true;");
    }

    @Test
    public void testIdentifierExprStatement() throws IOException {
        expectParses("""
                INT x;
                x;
                """);
    }

    @Test
    public void testParenthesizedExpr() throws IOException {
        expectParses("(1 + 2);");
    }

    @Test
    public void testUnaryMinus() throws IOException {
        expectParses("-5;");
    }

    @Test
    public void testUnaryNot() throws IOException {
        expectParses("not true;");
    }

    @Test
    public void testMultiplication() throws IOException {
        expectParses("2 * 3;");
    }

    @Test
    public void testDivision() throws IOException {
        expectParses("8 / 2;");
    }

    @Test
    public void testModulo() throws IOException {
        expectParses("9 % 4;");
    }

    @Test
    public void testAddition() throws IOException {
        expectParses("1 + 2;");
    }

    @Test
    public void testSubtraction() throws IOException {
        expectParses("5 - 3;");
    }

    @Test
    public void testRelationalExpr() throws IOException {
        expectParses("1 < 2;");
    }

    @Test
    public void testEqualityExpr() throws IOException {
        expectParses("1 == 1;");
    }

    @Test
    public void testInequalityExpr() throws IOException {
        expectParses("1 =/= 2;");
    }

    @Test
    public void testLogicalAndExpr() throws IOException {
        expectParses("true && false;");
    }

    @Test
    public void testLogicalOrExpr() throws IOException {
        expectParses("true || false;");
    }

    @Test
    public void testOperatorPrecedence() throws IOException {
        expectParses("1 + 2 * 3 < 10 && true || false;");
    }

    @Test
    public void testFunctionCallNoArgs() throws IOException {
        expectParses("foo();");
    }

    @Test
    public void testFunctionCallWithArgs() throws IOException {
        expectParses("foo(1, 2, 3);");
    }

    @Test
    public void testIndexAccess() throws IOException {
        expectParses("""
                ARRAY[INT] xs;
                xs[0];
                """);
    }

    @Test
    public void testFieldAccess() throws IOException {
        expectParses("""
                coll Person {
                    STRING name;
                }
                Person p;
                p.name;
                """);
    }

    @Test
    public void testChainedPostfixExpr() throws IOException {
        expectParses("foo(1)[2].bar;");
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidAssignmentTargetThrows() throws IOException {
        parse("(1 + 2) = 3;");
    }

    @Test(expected = RuntimeException.class)
    public void testMissingSemicolonAfterVarDeclThrows() throws IOException {
        parse("INT x");
    }

    @Test(expected = RuntimeException.class)
    public void testMissingRightParenInIfThrows() throws IOException {
        parse("""
                if (true {
                    return;
                }
                """);
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidTypeThrows() throws IOException {
        parse("ARRAY[] x;");
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidPrimaryExprThrows() throws IOException {
        parse(";");
    }

    @Test(expected = RuntimeException.class)
    public void testMissingCollectionNameThrows() throws IOException {
        parse("coll { INT x; }");
    }

    @Test(expected = RuntimeException.class)
    public void testMalformedArrayTypeThrows() throws IOException {
        parse("ARRAY[INT x;");
    }
}