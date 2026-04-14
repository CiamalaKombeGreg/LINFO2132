package compiler.Semantic;

public class SemanticException extends RuntimeException { // This exception is thrown when a semantic error is encountered during compilation
    public SemanticException(String keyword, String message) {
        super(keyword + ": " + message);
    }
}