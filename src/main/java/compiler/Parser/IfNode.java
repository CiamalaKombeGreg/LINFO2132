package compiler.Parser;

public class IfNode extends StatementNode {
    private final ExprNode condition;
    private final StatementNode thenBranch;
    private final StatementNode elseBranch;

    public IfNode(ExprNode condition, StatementNode thenBranch, StatementNode elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    public ExprNode getCondition() {
        return condition;
    }

    public StatementNode getThenBranch() {
        return thenBranch;
    }

    public StatementNode getElseBranch() {
        return elseBranch;
    }

    @Override
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("If\n");
        sb.append(indent).append("  Condition\n");
        sb.append(condition.toString(indent + "    "));
        sb.append(indent).append("  Then\n");
        sb.append(thenBranch.toString(indent + "    "));
        if (elseBranch != null) {
            sb.append(indent).append("  Else\n");
            sb.append(elseBranch.toString(indent + "    "));
        }
        return sb.toString();
    }
}