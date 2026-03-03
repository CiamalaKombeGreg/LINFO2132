import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;
import compiler.Lexer.SymbolType;

public class TestLexer {
    
    @Test
    public void test() throws IOException {
        String input = "private final INT x";
        StringReader reader = new StringReader(input);
        Lexer lexer = new Lexer(reader);
        assertNotNull(lexer.getNextSymbol());

        Symbol s1 = lexer.getNextSymbol();
        assertEquals(SymbolType.KEYWORD, s1.getType());
        assertEquals("final", s1.getValue());

        Symbol s2 = lexer.getNextSymbol();
        assertEquals(SymbolType.KEYWORD, s2.getType());
        assertEquals("INT", s2.getValue());

        Symbol s3 = lexer.getNextSymbol();
        assertEquals(SymbolType.IDENTIFIER, s3.getType());
        assertEquals("x", s3.getValue());

        Symbol s4 = lexer.getNextSymbol();
        assertEquals(SymbolType.EOF, s4.getType());
    }

}
