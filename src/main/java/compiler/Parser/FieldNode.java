package compiler.Parser;

public class FieldNode extends ASTNode {
    private final String type;
    private final String name;

    public FieldNode(String type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public String toString(String indent) {
        return indent + "Field, " + type + " " + name + "\n";
    }
}