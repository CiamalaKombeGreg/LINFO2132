package compiler.Parser;

public class BinaryExprNode extends ExprNode {
    private final String operator;
    private final ExprNode left;
    private final ExprNode right;

    public BinaryExprNode(String operator, ExprNode left, ExprNode right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    public String getOperator() {
        return operator;
    }

    public ExprNode getLeft() {
        return left;
    }

    public ExprNode getRight() {
        return right;
    }

    @Override
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("BinaryExpr, ").append(operator).append("\n");
        sb.append(left.toString(indent + "  "));
        sb.append(right.toString(indent + "  "));
        return sb.toString();
    }
}
