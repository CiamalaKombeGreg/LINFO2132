package compiler.Lexer;

/*
    * We will regroup symbols that will be verify in the same function.
    * Each function will describe the hierarchy of the symbols, which one is tested first.
*/

public class Symbol {
    final private SymbolType type;
    final private String value;

    // Constructor for creating a symbol with a type and an value (value is optional for certain tokens)
    public Symbol(SymbolType type, String value) {
        this.type = type;
        this.value = value;
    }

    // Return the type of the symbol
    public SymbolType getType() {
        return type;
    }

    // Return the value of the symbol (if necessary)
    public String getValue() {
        return value;
    }

    // Override toString for easy debugging and creating our token representation
    @Override
    public String toString() {
        if (value == null) {
            return "<" + type + ",>";
        }
        return "<" + type + ", " + value + ">";
    }
}
