package compiler.Semantic;

public class SymbolInfo {
    private final String name;
    private final Type type;
    private final SymbolKind kind;
    private final boolean isFinal;

    public SymbolInfo(String name, Type type, SymbolKind kind, boolean isFinal) {
        this.name = name; // Store the name of the symbol
        this.type = type; // Store the type of the symbol (e.g., int, float, class type)
        this.kind = kind; // Store the kind of the symbol (e.g., variable, function, parameter)
        this.isFinal = isFinal; // Store if the symbol is final (cannot be reassigned)
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public SymbolKind getKind() {
        return kind;
    }

    public boolean isFinal() {
        return isFinal;
    }
}