package compiler.Parser;

public class ReturnNode extends StatementNode {
    private final ExprNode expr;

    public ReturnNode(ExprNode expr) {
        this.expr = expr;
    }

    @Override
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("Return\n");
        if (expr != null) {
            sb.append(expr.toString(indent + "  "));
        }
        return sb.toString();
    }
}