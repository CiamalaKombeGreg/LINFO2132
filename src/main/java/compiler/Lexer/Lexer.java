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

        // Handle numbers and dots. A dot can be a DOT from the syntax or the start of a float.
        if (Character.isDigit(currentChar) || currentChar == '.') {
            return isSyntaxDot(currentChar);
        }

        // Handling string literals. We know they start with a double quote.
        if (currentChar == '"') {
            return lexString();
        }

        if (isSymbolStart(currentChar)) {
            skipWhitespace();
            return lexSymbol();
        }

        // Runtime exception for unrecognized characters
        throw new RuntimeException("Unrecognized character: " + (char) currentChar);
    }

    private boolean isSymbolStart(int c) {
        String symbols = "=+-*/%<>(){}[].&|;,";

        int position = symbols.indexOf(c);

        if (position == -1) {
            return false;
        } else {
            return true;
        }
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

        // Identifier is last because it can be a keyword or a collection name, so we check those first. If it's not a keyword or a collection name, then it's an identifier.
        return new Symbol(SymbolType.IDENTIFIER, word);
    }

    // Function to handle the dots so we know if it's a float or just a dot. lexNumber will handle the following digits and the fractional part if it's a float.
    private Symbol isSyntaxDot(int c) throws IOException {
        if (c == '.') {
            int next = input.read();
            // In case of EOF after a dot.
            if (next == -1) {
                return new Symbol(SymbolType.DOT, ".");
            }
            // If the next character is not a digit, then we consider it a DOT symbol.
            if (!Character.isDigit(next)) {
                unread(next);
                advance(); // We move past the dot since we don't need it anymore.
                return new Symbol(SymbolType.DOT, ".");
            }
            // If the next character is a digit, we push it back so that the lexNumber function can handle it as part of the float.
            unread(next);
        }
        return lexNumber();
    }

    // To handle numbers, we need to consider both integers and floats. A float can start with a dot or have a dot in the middle.
    private Symbol lexNumber() throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean isFloat = false; // We will know if we already encountered a dot in the number.

        // In case we start with a dot, we need to handle it as the start of a float.
        if (currentChar == '.') {
            isFloat = true;
            sb.append('.');
            advance();
        }

        // Handling digits before or after the dot (if any)
        while (currentChar != -1 && Character.isDigit(currentChar)) {
            sb.append((char) currentChar);
            advance();
        }

        // If we already have a dot and we encounter another dot.
        if (isFloat && currentChar == '.') {
            throw new RuntimeException("Malformed float literal (multiple dots)");
        }

        // In case we started with digits and then we encounter a dot. We only allow one dot in a number, so if we already have a dot, we don't consider it as part of the number.
        if (!isFloat && currentChar == '.') {
            isFloat = true;
            sb.append('.');
            advance();

            // Require at least one digit after dot. We avoid accepting something like "3." as valid.
            if (currentChar == -1 || !Character.isDigit(currentChar)) {
                throw new RuntimeException("Malformed float literal (missing digits after '.')");
            }

            // Handling digits after the dot.
            while (currentChar != -1 && Character.isDigit(currentChar)) {
                sb.append((char) currentChar);
                advance();
            }
        }

        String raw = sb.toString();

        if (!isFloat) {
            return new Symbol(SymbolType.INT, removeLeadingZeros(raw));
        } else {
            String normalized = raw;
            if (normalized.startsWith(".")) normalized = "0" + normalized; // Handle floats that start with a dot.

            // Normalize int part zeros: 0003.14 -> 3.14
            int dotIndex = normalized.indexOf('.');
            if (dotIndex > 0) {
                String intPart = removeLeadingZeros(normalized.substring(0, dotIndex));
                String fracPart = normalized.substring(dotIndex);
                normalized = intPart + fracPart;
            }
            return new Symbol(SymbolType.FLOAT, normalized);
        }
    }

    // Function to strip leading zeros from an integer string, for example "000123" -> "123".
    private String removeLeadingZeros(String s) {
        int i = 0;
        while (i < s.length() - 1 && s.charAt(i) == '0') i++;
        return s.substring(i);
    }

    private Symbol lexString() throws IOException {
        StringBuilder sb = new StringBuilder();
        advance(); // Let's skip the opening double quote

        while (currentChar != -1 && currentChar != '"') {
            if (currentChar == '\\') { // Handle escape sequences, we know they start with a backslash.
                advance();
                if (currentChar == -1) {
                    throw new RuntimeException("Incomplete string literal");
                }
                switch (currentChar) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '"' -> sb.append('\"');
                    case '\\' -> sb.append('\\');
                    default -> throw new RuntimeException("Invalid escape sequence: \\" + (char) currentChar);
                }
            } else {
                sb.append((char) currentChar);
            }
            advance();
        }

        if (currentChar == -1) {
            throw new RuntimeException("Incomplete string literal");
        }

        advance(); // Skip the closing double quote
        return new Symbol(SymbolType.STRING, sb.toString());
    }

    private void skipWhitespace() throws IOException {
        while (currentChar == ' ' || currentChar == '\t' || currentChar == '\n' || currentChar == '\r') {
            advance();
        }
    }

    private Symbol lexSymbol() throws IOException {
        int first = currentChar;
        advance();

        switch (first) {

            case '=':
                if (currentChar == '=') {
                    advance();
                    return new Symbol(SymbolType.EQ, "==");
                } else if (currentChar == '/') {
                    advance();
                    if (currentChar == '=') {
                        advance();
                        return new Symbol(SymbolType.NEQ, "=/=");
                    } else {
                        throw new RuntimeException("Invalid symbol");
                    }
                }
                return new Symbol(SymbolType.ASSIGNMENTOPERATOR, "=");

            case '<':
                if (currentChar == '=') {
                    advance();
                    return new Symbol(SymbolType.LE, "<=");
                }
                return new Symbol(SymbolType.LT, "<");

            case '>':
                if (currentChar == '=') {
                    advance();
                    return new Symbol(SymbolType.GE, ">=");
                }
                return new Symbol(SymbolType.GT, ">");

            case '&':
                if (currentChar == '&') {
                    advance();
                    return new Symbol(SymbolType.AND, "&&");
                }
                throw new RuntimeException("Invalid symbol");

            case '|':
                if (currentChar == '|') {
                    advance();
                    return new Symbol(SymbolType.OR, "||");
                }
                throw new RuntimeException("Invalid symbol");

            // Simple Operator

            case '+':
                return new Symbol(SymbolType.PLUS, "+");

            case '-':
                return new Symbol(SymbolType.MINUS, "-");

            case '*':
                return new Symbol(SymbolType.MULT, "*");

            case '/':
                return new Symbol(SymbolType.DIV, "/");

            case '%':
                return new Symbol(SymbolType.MOD, "%");

            // Delimiters

            case '(':
                return new Symbol(SymbolType.LPAREN, "(");

            case ')':
                return new Symbol(SymbolType.RPAREN, ")");

            case '{':
                return new Symbol(SymbolType.LBRACE, "{");

            case '}':
                return new Symbol(SymbolType.RBRACE, "}");

            case '[':
                return new Symbol(SymbolType.LBRACKET, "[");

            case ']':
                return new Symbol(SymbolType.RBRACKET, "]");

            case ';':
                return new Symbol(SymbolType.SEMICOLON, ";");

            case ',':
                return new Symbol(SymbolType.COMMA, ",");

            case '.':
                return new Symbol(SymbolType.DOT, ".");

            default:
                throw new RuntimeException("Unrecognized symbol");
        }
    }
}
