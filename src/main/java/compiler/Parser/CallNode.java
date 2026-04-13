package compiler.Parser;

import java.util.List;

public class CallNode extends ExprNode {
    private final ExprNode callee;
    private final List<ExprNode> args;

    public CallNode(ExprNode callee, List<ExprNode> args) {
        this.callee = callee;
        this.args = args;
    }

    @Override
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("Call\n");
        sb.append(indent).append("  Callee\n");
        sb.append(callee.toString(indent + "    "));
        for (ExprNode arg : args) {
            sb.append(indent).append("  Arg\n");
            sb.append(arg.toString(indent + "    "));
        }
        return sb.toString();
    }
}