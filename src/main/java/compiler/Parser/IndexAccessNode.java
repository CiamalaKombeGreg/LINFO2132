package compiler.Parser;

public class IndexAccessNode extends ExprNode {
    private final ExprNode target;
    private final ExprNode index;

    public IndexAccessNode(ExprNode target, ExprNode index) {
        this.target = target;
        this.index = index;
    }

    @Override
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("IndexAccess\n");
        sb.append(indent).append("  Target\n");
        sb.append(target.toString(indent + "    "));
        sb.append(indent).append("  Index\n");
        sb.append(index.toString(indent + "    "));
        return sb.toString();
    }
}