package compiler.Parser;

import java.util.ArrayList;
import java.util.List;

public class ProgramNode extends ASTNode {
    private final List<ASTNode> elements = new ArrayList<>();

    public void add(ASTNode node) {
        elements.add(node);
    }

    public List<ASTNode> getElements() {
        return elements;
    }

    @Override
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("Program\n");
        for (ASTNode node : elements) {
            sb.append(node.toString(indent + "  "));
        }
        return sb.toString();
    }
}