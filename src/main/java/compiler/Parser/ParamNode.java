package compiler.Parser;

public class ParamNode extends ASTNode {
    private final String type;
    private final String name;

    public ParamNode(String type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public String toString(String indent) {
        return indent + "Param, " + type + " " + name + "\n";
    }
}