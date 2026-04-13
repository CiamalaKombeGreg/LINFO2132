package compiler.Parser;

import java.util.List;

public class FunctionDefNode extends ASTNode {
    private final String returnType;
    private final String name;
    private final List<ParamNode> params;
    private final BlockNode body;

    public FunctionDefNode(String returnType, String name, List<ParamNode> params, BlockNode body) {
        this.returnType = returnType;
        this.name = name;
        this.params = params;
        this.body = body;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getName() {
        return name;
    }

    public List<ParamNode> getParams() {
        return params;
    }

    public BlockNode getBody() {
        return body;
    }

    @Override
    public String toString(String indent) {
        StringBuilder sb = new StringBuilder();

        sb.append(indent).append("FunctionDef, ");
        if (returnType != null) {
            sb.append(returnType).append(" ");
        }
        sb.append(name).append("\n");

        for (ParamNode p : params) {
            sb.append(p.toString(indent + "  "));
        }
        sb.append(body.toString(indent + "  "));
        return sb.toString();
    }
}
