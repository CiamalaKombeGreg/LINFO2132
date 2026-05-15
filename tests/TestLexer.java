import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;
import compiler.Lexer.SymbolType;

public class TestLexer {

    private Lexer lex(String input) throws IOException {
        return new Lexer(new StringReader(input));
    }

    private void expect(Lexer lexer, SymbolType type, String value) throws IOException {
        Symbol s = lexer.getNextSymbol();
        assertNotNull(s);
        assertEquals(type, s.getType());
        assertEquals(value, s.getValue());
    }

    // For operators/punctuation
    private void expectOp(Lexer lexer, SymbolType type, String maybeValue) throws IOException {
        Symbol s = lexer.getNextSymbol();
        assertNotNull(s);
        assertEquals(type, s.getType());
        if (s.getValue() != null) assertEquals(maybeValue, s.getValue());
    }

    private void expectType(Lexer lexer, SymbolType type) throws IOException {
        Symbol s = lexer.getNextSymbol();
        assertNotNull(s);
        assertEquals(type, s.getType());
    }

    @Test
    public void testCoreHappyPathGrouped() throws IOException {
        String input =
                """
                # comment
                final INT x = 00342;
                FLOAT y = .234;
                BOOL b = true;
                STRING s = "He said: \\"hi\\"";
                List t = (x <= 10) && (b || false);
                a.b;""";

        Lexer lexer = lex(input);

        // final INT x = 00342;
        expect(lexer, SymbolType.KEYWORD, "final");
        expect(lexer, SymbolType.KEYWORD, "INT");
        expect(lexer, SymbolType.IDENTIFIER, "x");
        expectOp(lexer, SymbolType.ASSIGNMENTOPERATOR, "=");
        expect(lexer, SymbolType.INT, "342");
        expectOp(lexer, SymbolType.SEMICOLON, ";");

        // FLOAT y = .234;
        expect(lexer, SymbolType.KEYWORD, "FLOAT");
        expect(lexer, SymbolType.IDENTIFIER, "y");
        expectOp(lexer, SymbolType.ASSIGNMENTOPERATOR, "=");
        expect(lexer, SymbolType.FLOAT, "0.234");
        expectOp(lexer, SymbolType.SEMICOLON, ";");

        // BOOL b = true;
        expect(lexer, SymbolType.KEYWORD, "BOOL");
        expect(lexer, SymbolType.IDENTIFIER, "b");
        expectOp(lexer, SymbolType.ASSIGNMENTOPERATOR, "=");
        expect(lexer, SymbolType.BOOLEAN, "true");
        expectOp(lexer, SymbolType.SEMICOLON, ";");

        // STRING s = "He said: \"hi\"";
        expect(lexer, SymbolType.KEYWORD, "STRING");
        expect(lexer, SymbolType.IDENTIFIER, "s");
        expectOp(lexer, SymbolType.ASSIGNMENTOPERATOR, "=");
        expect(lexer, SymbolType.STRING, "He said: \"hi\"");
        expectOp(lexer, SymbolType.SEMICOLON, ";");

        // List t = (x <= 10) && (b || false);
        expect(lexer, SymbolType.COLLECTION_NAME, "List");
        expect(lexer, SymbolType.IDENTIFIER, "t");
        expectOp(lexer, SymbolType.ASSIGNMENTOPERATOR, "=");

        expectOp(lexer, SymbolType.LPAREN, "(");
        expect(lexer, SymbolType.IDENTIFIER, "x");
        expectOp(lexer, SymbolType.LE, "<=");
        expect(lexer, SymbolType.INT, "10");
        expectOp(lexer, SymbolType.RPAREN, ")");

        expectOp(lexer, SymbolType.AND, "&&");

        expectOp(lexer, SymbolType.LPAREN, "(");
        expect(lexer, SymbolType.IDENTIFIER, "b");
        expectOp(lexer, SymbolType.OR, "||");
        expect(lexer, SymbolType.BOOLEAN, "false");
        expectOp(lexer, SymbolType.RPAREN, ")");

        expectOp(lexer, SymbolType.SEMICOLON, ";");

        // a.b;
        expect(lexer, SymbolType.IDENTIFIER, "a");
        expectOp(lexer, SymbolType.DOT, ".");
        expect(lexer, SymbolType.IDENTIFIER, "b");
        expectOp(lexer, SymbolType.SEMICOLON, ";");

        expectType(lexer, SymbolType.EOF);
    }

    @Test(expected = RuntimeException.class)
    public void testUnknownCharThrows() throws IOException {
        Lexer lexer = lex("INT x = @;");
        while (lexer.getNextSymbol().getType() != SymbolType.EOF) {
            // keep consuming until it throws
        }
    }

    @Test(expected = RuntimeException.class)
    public void testUnterminatedStringThrows() throws IOException {
        Lexer lexer = lex("\"hello");
        lexer.getNextSymbol();
    }
}