package compiler.Parser;

public class UnaryExprNode extends ExprNode {
    private final String operator;
    private final ExprNode expr;

    public UnaryExprNode(String operator, ExprNode expr) {
        this.operator = operator;
        this.expr = expr;
    }

    public String getOperator() {
        return operator;
    }

    public ExprNode getExpr() {
        return expr;
    }

    @Override
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("UnaryExpr, ").append(operator).append("\n");
        sb.append(expr.toString(indent + "  "));
        return sb.toString();
    }
}
