package compiler.Parser;

public class WhileNode extends StatementNode {
    private final ExprNode condition;
    private final StatementNode body;

    public WhileNode(ExprNode condition, StatementNode body) {
        this.condition = condition;
        this.body = body;
    }

    @Override
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("While\n");
        sb.append(indent).append("  Condition\n");
        sb.append(condition.toString(indent + "    "));
        sb.append(indent).append("  Body\n");
        sb.append(body.toString(indent + "    "));
        return sb.toString();
    }
}