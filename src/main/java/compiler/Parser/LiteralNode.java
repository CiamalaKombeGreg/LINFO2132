package compiler.Parser;

public class LiteralNode extends ExprNode {
    private final String kind;
    private final String value;

    public LiteralNode(String kind, String value) {
        this.kind = kind;
        this.value = value;
    }

    public String getKind() {
        return kind;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString(String indent) {
        return indent + kind + ", " + value + "\n";
    }
}