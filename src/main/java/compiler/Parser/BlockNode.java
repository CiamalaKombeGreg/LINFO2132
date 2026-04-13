package compiler.Parser;

import java.util.ArrayList;
import java.util.List;

public class BlockNode extends StatementNode {
    private final List<StatementNode> statements = new ArrayList<>();

    public void add(StatementNode stmt) {
        statements.add(stmt);
    }

    @Override
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("Block\n");
        for (StatementNode stmt : statements) {
            sb.append(stmt.toString(indent + "  "));
        }
        return sb.toString();
    }
}