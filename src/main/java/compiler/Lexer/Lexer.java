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

    // Advance to the next character in the input, I made a function since we will need to do this multiple times
    private void advance() {
        try {
            currentChar = input.read();
        } catch (IOException e) {
            currentChar = -1; // EOF
        }
    }
    
    // Method to get the next symbol from the input
    public Symbol getNextSymbol() {

        // Skip whitespace & comment
        while (true) {

            // Skip whitespace
            while (Character.isWhitespace(currentChar)) {
                advance();
            }

            // Handle comment
            if (currentChar == '#') {
                while (currentChar != '\n' && currentChar != -1) {
                    advance();
                }
                continue;  // restart the loop so we directly handle the next line after the comment without returning to the main loop
            }

            break;  // We exit the loop when we don't encounter whitespaces or comments
        }


        // Signal end of file
        if (currentChar == -1) {
            return new Symbol(SymbolType.EOF, null);
        }

        // Runtime exception for unrecognized characters
        throw new RuntimeException("Unrecognized character: " + (char) currentChar);
    }
}
