package compiler.Lexer;

import java.io.IOException;
import java.io.Reader;

public class Lexer {

    private final Reader input; //Input source for the lexer
    private int currentChar; // Current character being analyzed
    
    // Constructor to initialize the lexer with an input source
    public Lexer(Reader input) {
        this.input = input;
        advance(); // Advance to the first character
    }

    // Advance to the next character in the input
    private void advance() {
        try {
            currentChar = input.read();
        } catch (IOException e) {
            currentChar = -1; // EOF
        }
    }
    
    public Symbol getNextSymbol() {
        return null;
    }
}
