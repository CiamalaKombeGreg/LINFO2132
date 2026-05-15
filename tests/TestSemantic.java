import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import compiler.Lexer.Lexer;
import compiler.Parser.ASTNode;
import compiler.Parser.Parser;
import compiler.Semantic.SemanticAnalyzer;

public class TestSemantic {

    private ASTNode parse(String input) throws IOException {
        Lexer lexer = new Lexer(new StringReader(input));
        Parser parser = new Parser(lexer);
        return parser.getAST();
    }

    private void expectSemanticPasses(String input) throws IOException {
        ASTNode ast = parse(input);
        assertNotNull(ast);

        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        analyzer.analyze(ast);
    }

    private void expectSemanticError(String input, String keyword) throws IOException {
        ASTNode ast = parse(input);
        assertNotNull(ast);

        try {
            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            analyzer.analyze(ast);
        } catch (RuntimeException e) {
            assertTrue("Expected error message to contain keyword: " + keyword
                    + " but got: " + e.getMessage(),
                    e.getMessage() != null && e.getMessage().contains(keyword));
            return;
        }

        throw new AssertionError("Expected semantic error containing keyword: " + keyword);
    }

    @Test
    public void testSemanticHappyPath() throws IOException {
        expectSemanticPasses("""
                coll Person {
                    STRING name;
                    INT age;
                }

                def INT add(INT a, INT b) {
                    INT c = a + b;
                    return c;
                }

                def main() {
                    INT x = 3;
                    INT y = 4;
                    INT z = add(x, y);
                }
                """);
    }

    @Test
    public void testTypeErrorAssignment() throws IOException {
        expectSemanticError("""
                def main() {
                    INT x = "hello";
                }
                """, "TypeError");
    }

    @Test
    public void testCollectionErrorDuplicateTypeName() throws IOException {
        expectSemanticError("""
                coll Person {
                    STRING name;
                }

                coll Person {
                    INT age;
                }
                """, "CollectionError");
    }

    @Test
    public void testOperatorErrorArithmeticMismatch() throws IOException {
        expectSemanticError("""
                def main() {
                    INT x = 3 + 4.5;
                }
                """, "OperatorError");
    }

    @Test
    public void testOperatorErrorBooleanArithmetic() throws IOException {
        expectSemanticError("""
                def main() {
                    BOOL b = true;
                    BOOL c = false;
                    INT x = b + c;
                }
                """, "OperatorError");
    }

    @Test
    public void testArgumentErrorWrongFunctionArgumentType() throws IOException {
        expectSemanticError("""
                def INT square(INT x) {
                    return x * x;
                }

                def main() {
                    INT y = square("hello");
                }
                """, "ArgumentError");
    }

    @Test
    public void testArgumentErrorWrongFunctionArgumentCount() throws IOException {
        expectSemanticError("""
                def INT add(INT a, INT b) {
                    return a + b;
                }

                def main() {
                    INT x = add(1);
                }
                """, "ArgumentError");
    }

    @Test
    public void testMissingConditionErrorIf() throws IOException {
        expectSemanticError("""
                def main() {
                    if (3) {
                        INT x = 1;
                    }
                }
                """, "MissingConditionError");
    }

    @Test
    public void testMissingConditionErrorWhile() throws IOException {
        expectSemanticError("""
                def main() {
                    while (5) {
                        INT x = 1;
                    }
                }
                """, "MissingConditionError");
    }

    @Test
    public void testReturnErrorWrongReturnType() throws IOException {
        expectSemanticError("""
                def INT f() {
                    return "hello";
                }
                """, "ReturnError");
    }

    @Test
    public void testReturnErrorMissingReturn() throws IOException {
        expectSemanticError("""
                def INT f() {
                    INT x = 3;
                }
                """, "ReturnError");
    }

    @Test
    public void testScopeErrorUnknownIdentifier() throws IOException {
        expectSemanticError("""
                def main() {
                    INT x = y;
                }
                """, "ScopeError");
    }

    @Test
    public void testScopeErrorDuplicateLocalDeclaration() throws IOException {
        expectSemanticError("""
                def main() {
                    INT x = 1;
                    INT x = 2;
                }
                """, "ScopeError");
    }

    @Test
    public void testScopeErrorDuplicateParameter() throws IOException {
        expectSemanticError("""
                def INT f(INT x, INT x) {
                    return x;
                }
                """, "ScopeError");
    }

    @Test
    public void testTypeErrorAssignToFinalVariable() throws IOException {
        expectSemanticError("""
                def main() {
                    final INT x = 3;
                    x = 4;
                }
                """, "TypeError");
    }

    @Test
    public void testCollectionConstructorArgumentError() throws IOException {
        expectSemanticError("""
                coll Person {
                    STRING name;
                    INT age;
                }

                def main() {
                    Person p = Person("Alice", "wrong");
                }
                """, "ArgumentError");
    }

    @Test
    public void testFieldAccessHappyPath() throws IOException {
        expectSemanticPasses("""
                coll Person {
                    STRING name;
                    INT age;
                }

                def main() {
                    Person p = Person("Alice", 20);
                    STRING s = p.name;
                }
                """);
    }

    @Test
    public void testIndexAccessHappyPath() throws IOException {
        expectSemanticPasses("""
                def main() {
                    ARRAY[INT] xs;
                    INT y = xs[0];
                }
                """);
    }
}