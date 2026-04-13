package compiler.Parser;

import java.util.ArrayList;
import java.util.List;

public class CollDeclNode extends ASTNode {
    private final String name;
    private final List<FieldNode> fields = new ArrayList<>();

    public CollDeclNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<FieldNode> getFields() {
        return fields;
    }

    public void addField(FieldNode field) {
        fields.add(field);
    }

    @Override
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("CollDecl, ").append(name).append("\n");

        for (FieldNode f : fields) {
            sb.append(f.toString(indent + "  "));
        }

        return sb.toString();
    }
}