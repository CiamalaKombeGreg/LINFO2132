package compiler.Lexer;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;

public class Lexer {

    private final PushbackReader input; // For handling lookahead when needed
    private int currentChar; // Current character being analyzed

    private static final java.util.Set<String> Keywords = java.util.Set.of(
        "final", "coll", "def", "for", "while", "if", "else", "return", "not", "ARRAY","INT", "FLOAT", "BOOL", "STRING"
    );
    
    // Constructor to initialize the lexer with an input source
    public Lexer(Reader input) {
        this.input = new PushbackReader(input, 2); // We use a PushbackReader to allow us to "unread" characters for lookahead, buffer size is 2.
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

    // Pushes back ONE character into the stream so that it can be read again. This is useful for lookahead.
    private void unread(int c) {
        if (c != -1) try {
            input.unread(c);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    // Method to get the next symbol from the input
    public Symbol getNextSymbol() throws IOException {

        // Skip whitespace & comment
        skipWhitespaceAndComments();

        // Signal end of file
        if (currentChar == -1) {
            return new Symbol(SymbolType.EOF, null);
        }

        // Handle literals, keywords, and identifiers.
        if (isLiteral(currentChar)) {
            return lexWord();
        }

        // Runtime exception for unrecognized characters
        throw new RuntimeException("Unrecognized character: " + (char) currentChar);
    }

    // Function to skip whitespace and comments.
    private void skipWhitespaceAndComments() {
        while (true) {
            // whitespace
            while (currentChar != -1 && Character.isWhitespace(currentChar)) {
                advance();
            }

            // comment
            if (currentChar == '#') {
                while (currentChar != -1 && currentChar != '\n') {
                    advance();
                }
                continue; // We do a loop here because there could be multiple comments or whitespace.
            }

            break;
        }
    }

    // Handling the keywords, identifiers, and collection names. We know they start with a letter or an underscore.
    private boolean isLiteral(int c) {
        return Character.isLetter(c) || c == '_';
    }

    // Function to lex a word who will handle keywords, identifiers, and collection names.
    private Symbol lexWord() throws IOException {
        StringBuilder sb = new StringBuilder();

        while (currentChar != -1 &&
                (Character.isLetterOrDigit(currentChar) || currentChar == '_')) {
            sb.append((char) currentChar);
            advance();
        }

        String word = sb.toString();

        if (word.equals("true") || word.equals("false")) {
            return new Symbol(SymbolType.BOOLEAN, word);
        }

        if (Keywords.contains(word)) {
            return new Symbol(SymbolType.KEYWORD, word);
        }

        // If the first character is uppercase, we consider it a collection name.
        char first = word.charAt(0);
        if (Character.isUpperCase(first)) {
            return new Symbol(SymbolType.COLLECTION_NAME, word);
        }

        return new Symbol(SymbolType.IDENTIFIER, word);
    }

}
