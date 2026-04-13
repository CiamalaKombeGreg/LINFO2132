package compiler.Parser;

public class AssignmentNode extends StatementNode {
    private final ExprNode target;
    private final ExprNode value;

    public AssignmentNode(ExprNode target, ExprNode value) {
        this.target = target;
        this.value = value;
    }

    @Override
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("Assignment\n");
        sb.append(indent).append("  Target\n");
        sb.append(target.toString(indent + "    "));
        sb.append(indent).append("  Value\n");
        sb.append(value.toString(indent + "    "));
        return sb.toString();
    }
}