package compiler.Parser;

public class FieldAccessNode extends ExprNode {
    private final ExprNode target;
    private final String fieldName;

    public FieldAccessNode(ExprNode target, String fieldName) {
        this.target = target;
        this.fieldName = fieldName;
    }

    public ExprNode getTarget() {
        return target;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("FieldAccess, ").append(fieldName).append("\n");
        sb.append(target.toString(indent + "  "));
        return sb.toString();
    }
}