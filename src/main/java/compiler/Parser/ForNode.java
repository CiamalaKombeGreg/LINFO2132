package compiler.Parser;

public class ForNode extends StatementNode {
    private final ASTNode init;
    private final ExprNode condition;
    private final ASTNode update;
    private final StatementNode body;

    public ForNode(ASTNode init, ExprNode condition, ASTNode update, StatementNode body) {
        this.init = init;
        this.condition = condition;
        this.update = update;
        this.body = body;
    }

    public ASTNode getInit() {
        return init;
    }

    public ExprNode getCondition() {
        return condition;
    }

    public ASTNode getUpdate() {
        return update;
    }

    public StatementNode getBody() {
        return body;
    }

    @Override
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("For\n");

        if (init != null) {
            sb.append(indent).append("  Init\n");
            sb.append(init.toString(indent + "    "));
        }

        if (condition != null) {
            sb.append(indent).append("  Condition\n");
            sb.append(condition.toString(indent + "    "));
        }

        if (update != null) {
            sb.append(indent).append("  Update\n");
            sb.append(update.toString(indent + "    "));
        }

        sb.append(indent).append("  Body\n");
        sb.append(body.toString(indent + "    "));
        return sb.toString();
    }
}
