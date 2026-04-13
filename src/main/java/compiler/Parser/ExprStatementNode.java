package compiler.Parser;

public class ExprStatementNode extends StatementNode {
    private final ExprNode expr;

    public ExprStatementNode(ExprNode expr) {
        this.expr = expr;
    }

    @Override
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("ExprStatement\n");
        sb.append(expr.toString(indent + "  "));
        return sb.toString();
    }
}