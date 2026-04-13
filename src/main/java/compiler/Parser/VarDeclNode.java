package compiler.Parser;

public class VarDeclNode extends StatementNode {
    private final boolean isFinal;
    private final String type;
    private final String name;
    private final ExprNode initializer;

    public VarDeclNode(boolean isFinal, String type, String name, ExprNode initializer) {
        this.isFinal = isFinal;
        this.type = type;
        this.name = name;
        this.initializer = initializer;
    }

    @Override
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("VarDecl\n");
        if (isFinal) {
            sb.append(indent).append("  Final\n");
        }
        sb.append(indent).append("  Type, ").append(type).append("\n");
        sb.append(indent).append("  Identifier, ").append(name).append("\n");
        if (initializer != null) {
            sb.append(indent).append("  Initializer\n");
            sb.append(initializer.toString(indent + "    "));
        }
        return sb.toString();
    }
}