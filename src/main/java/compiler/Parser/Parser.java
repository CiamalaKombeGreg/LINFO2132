package compiler.Parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import compiler.Lexer.Lexer;
import compiler.Lexer.Symbol;
import compiler.Lexer.SymbolType;

public class Parser {
    private final Lexer lexer;
    private Symbol current;

    // Builds the parser and immediately loads the first token.
    public Parser(Lexer lexer) throws IOException {
        this.lexer = lexer; // Keep the lexer to ask for tokens.
        advance(); // Load the first token so parsing can start.
    }

    // Moves to the next token produced by the lexer.
    private void advance() throws IOException {
        current = lexer.getNextSymbol(); // Replace current token by the next one.
    }

    // Checks if the current token has the given type.
    private boolean check(SymbolType type) {
        return current.getType() == type; // True if current token matches the expected type.
    }

    // Verifies the current token type, consumes it, and returns it.
    private Symbol match(SymbolType type) throws IOException {
        if (!check(type)) {
            throw new RuntimeException("Expected " + type + " but got " + current); // Syntax error if wrong token.
        }
        Symbol matched = current; // Save the token before consuming it.
        advance(); // Move to next token after a successful match.
        return matched; // Return the consumed token.
    }

    // Checks if the current token is a specific keyword.
    private boolean checkKeyword(String keyword) {
        return current.getType() == SymbolType.KEYWORD && // Token must be a keyword
               keyword.equals(current.getValue()); // and its value must be the right word.
    }

    // Verifies a specific keyword, consumes it, and returns it.
    private Symbol matchKeyword(String keyword) throws IOException {
        if (!checkKeyword(keyword)) {
            throw new RuntimeException("Expected keyword " + keyword + " but got " + current); // Syntax error if wrong keyword.
        }
        Symbol matched = current; // Save current keyword.
        advance(); // Consume it.
        return matched;
    }

    // Entry point of the parser: parses the whole file and returns the AST root.
    public ASTNode getAST() throws IOException {
        ASTNode root = parseProgram(); // Parse the whole input as a program.
        match(SymbolType.EOF); // Ensure nothing remains after the program.
        return root; // Return the AST root.
    }

    // Parses the whole program: top-level statements, functions, and collections.
    private ProgramNode parseProgram() throws IOException {
        ProgramNode program = new ProgramNode(); // Root node containing everything.

        while (!check(SymbolType.EOF)) { // Keep parsing until end of file.
            if (checkKeyword("coll")) {
                program.add(parseCollDecl()); // Parse a collection declaration.
            } else if (checkKeyword("def")) {
                program.add(parseFunctionDef()); // Parse a function definition.
            } else {
                program.add(parseStatement()); // Otherwise parse a normal statement.
            }
        }

        return program;
    }

    // Checks if the current token can start a variable declaration.
    private boolean isStartOfVarDecl() {
        if (checkKeyword("final")) {
            return true; // "final" always starts a declaration.
        }

        if (current.getType() == SymbolType.KEYWORD) {
            String v = current.getValue(); // Read the keyword text.
            return "INT".equals(v)
                || "FLOAT".equals(v)
                || "STRING".equals(v)
                || "BOOL".equals(v)
                || "ARRAY".equals(v); // Built-in type names can start declarations.
        }

        return current.getType() == SymbolType.COLLECTION_NAME; // User-defined collection types can also start declarations.
    }

    // Parses one statement of the language.
    private StatementNode parseStatement() throws IOException {
        if (check(SymbolType.LBRACE)) {
            return parseBlock();
        }

        if (checkKeyword("if")) {
            return parseIfStmt();
        }

        if (checkKeyword("while")) {
            return parseWhileStmt();
        }

        if (checkKeyword("return")) {
            ReturnNode ret = parseReturnStmt();
            match(SymbolType.SEMICOLON);
            return ret;
        }

        if (checkKeyword("for")) {
            return parseForStmt();
        }

        if (isStartOfVarDecl()) {
            VarDeclNode decl = parseVarDecl();
            match(SymbolType.SEMICOLON);
            return decl;
        }

        // Expression / assignment
        ExprNode left = parseExpr();

        if (check(SymbolType.ASSIGNMENTOPERATOR)) {
            if (!isAssignable(left)) {
                throw new RuntimeException("Invalid assignment target");
            }

            advance();
            ExprNode value = parseExpr();
            match(SymbolType.SEMICOLON);
            return new AssignmentNode(left, value);
        }

        match(SymbolType.SEMICOLON);
        return new ExprStatementNode(left);
    }

    // Checks whether an expression is valid on the left-hand side of '='.
    private boolean isAssignable(ExprNode expr) {
        return expr instanceof IdentifierNode
            || expr instanceof IndexAccessNode
            || expr instanceof FieldAccessNode; // Variables, array accesses, and field accesses are assignable.
    }

    // Parses a block: a list of statements between '{' and '}'.
    private BlockNode parseBlock() throws IOException {
        match(SymbolType.LBRACE); // Consume '{'.

        BlockNode block = new BlockNode(); // Create the block node.

        while (!check(SymbolType.RBRACE)) { // Parse statements until '}'.
            block.add(parseStatement());
        }

        match(SymbolType.RBRACE); // Consume '}'.
        return block;
    }

    // Parses a variable declaration, with optional "final" and optional initializer.
    private VarDeclNode parseVarDecl() throws IOException {
        boolean isFinal = false;

        if (checkKeyword("final")) {
            isFinal = true; // Remember that this declaration is immutable.
            advance(); // Consume 'final'.
        }

        String type = parseTypeName(); // Parse the declared type.
        String name = match(SymbolType.IDENTIFIER).getValue(); // Parse variable name.

        ExprNode initializer = null;
        if (check(SymbolType.ASSIGNMENTOPERATOR)) {
            advance(); // Consume '=' if there is an initializer.
            initializer = parseExpr(); // Parse the initializer expression.
        }

        return new VarDeclNode(isFinal, type, name, initializer);
    }

    // Parses a type name: built-in type, array type, or collection type.
    // Parses a type name: built-in type, array type, or collection type.
    private String parseTypeName() throws IOException {
        String baseType;

        if (null == current.getType()) {
            throw new RuntimeException("Expected type but got " + current); // Not a valid type.
        } else switch (current.getType()) {
            case KEYWORD -> {
                String value = current.getValue(); // Read the keyword text.
                if (null
                        == value) {
                    throw new RuntimeException("Expected type but got " + current); // Not a valid type.
                } else switch (value) {
                case "INT", "FLOAT", "STRING", "BOOL", "BOOLEAN" -> {
                    advance(); // Consume the simple built-in type.
                    baseType = value;
                    }
                case "ARRAY" -> {
                    advance(); // Consume 'ARRAY'.
                    match(SymbolType.LBRACKET); // Expect '['.
                    String inner = parseTypeName(); // Parse the inner type recursively.
                    match(SymbolType.RBRACKET); // Expect ']'.
                    baseType = "ARRAY[" + inner + "]"; // Build array type name.
                    }
                default -> throw new RuntimeException("Expected type but got " + current); // Not a valid type.
            }
            }
            case COLLECTION_NAME -> {
                baseType = current.getValue(); // Read user-defined type name.
                advance(); // Consume it.
            }
            default -> throw new RuntimeException("Expected type but got " + current); // Not a valid type.
        }

        // Support postfix array syntax like INT[] or Person[]
        while (check(SymbolType.LBRACKET)) {
            advance(); // Consume '['.
            match(SymbolType.RBRACKET); // Expect ']'.
            baseType = "ARRAY[" + baseType + "]"; // Convert postfix [] into ARRAY[...]
        }

        return baseType;
    }

    // Top expression entry point: starts from the lowest-priority level.
    private ExprNode parseExpr() throws IOException {
        return parseRangeExpr(); // Range is now the outermost precedence level here.
    }

    // Parses range expressions: a -> b.
    private ExprNode parseRangeExpr() throws IOException {
        ExprNode left = parseOrExpr(); // Parse the left side first.

        while (check(SymbolType.ARROW)) {
            advance(); // Consume '->'.
            ExprNode right = parseOrExpr(); // Parse right side of the range.
            left = new BinaryExprNode("->", left, right); // Build range AST node.
        }

        return left;
    }

    // Parses logical OR expressions: a || b || c.
    private ExprNode parseOrExpr() throws IOException {
        ExprNode left = parseAndExpr(); // First parse stronger-precedence expression.

        while (check(SymbolType.OR)) {
            advance(); // Consume '||'.
            ExprNode right = parseAndExpr(); // Parse right operand.
            left = new BinaryExprNode("||", left, right); // Build left-associative AST.
        }

        return left;
    }

    // Parses logical AND expressions: a && b && c.
    private ExprNode parseAndExpr() throws IOException {
        ExprNode left = parseEqualityExpr(); // AND is below equality in precedence.

        while (check(SymbolType.AND)) {
            advance(); // Consume '&&'.
            ExprNode right = parseEqualityExpr(); // Parse right operand.
            left = new BinaryExprNode("&&", left, right);
        }

        return left;
    }

    // Parses equality expressions: == and =/=.
    private ExprNode parseEqualityExpr() throws IOException {
        ExprNode left = parseRelExpr(); // Equality compares relational expressions.

        while (check(SymbolType.EQ) || check(SymbolType.NEQ)) {
            String op = check(SymbolType.EQ) ? "==" : "=/="; // Choose the correct operator text.
            advance(); // Consume the operator.
            ExprNode right = parseRelExpr(); // Parse right operand.
            left = new BinaryExprNode(op, left, right);
        }

        return left;
    }

    // Parses comparison expressions: <, >, <=, >=.
    private ExprNode parseRelExpr() throws IOException {
        ExprNode left = parseAddExpr(); // Comparisons compare arithmetic expressions.

        while (check(SymbolType.LT)
            || check(SymbolType.GT)
            || check(SymbolType.LE)
            || check(SymbolType.GE)) {

            String op;
            if (check(SymbolType.LT)) op = "<";
            else if (check(SymbolType.GT)) op = ">";
            else if (check(SymbolType.LE)) op = "<=";
            else op = ">="; // Determine which comparison operator is present.

            advance(); // Consume the operator.
            ExprNode right = parseAddExpr(); // Parse right operand.
            left = new BinaryExprNode(op, left, right);
        }

        return left;
    }

    // Parses addition and subtraction: + and -.
    private ExprNode parseAddExpr() throws IOException {
        ExprNode left = parseMulExpr(); // Addition is lower precedence than multiplication.

        while (check(SymbolType.PLUS) || check(SymbolType.MINUS)) {
            String op = check(SymbolType.PLUS) ? "+" : "-"; // Choose operator text.
            advance(); // Consume operator.
            ExprNode right = parseMulExpr(); // Parse right operand.
            left = new BinaryExprNode(op, left, right);
        }

        return left;
    }

    // Parses multiplication, division, and modulo: *, /, %.
    private ExprNode parseMulExpr() throws IOException {
        ExprNode left = parseUnaryExpr(); // Multiplication is lower precedence than unary operators.

        while (check(SymbolType.MULT) || check(SymbolType.DIV) || check(SymbolType.MOD)) {
            String op;
            if (check(SymbolType.MULT)) op = "*";
            else if (check(SymbolType.DIV)) op = "/";
            else op = "%"; // Determine which multiplicative operator is present.

            advance(); // Consume operator.
            ExprNode right = parseUnaryExpr(); // Parse right operand.
            left = new BinaryExprNode(op, left, right);
        }

        return left;
    }

    // Parses unary operators like -x and not x.
    private ExprNode parseUnaryExpr() throws IOException {
        if (check(SymbolType.MINUS)) {
            advance(); // Consume unary '-'.
            return new UnaryExprNode("-", parseUnaryExpr()); // Recursive call allows chaining like --x.
        }

        if (checkKeyword("not")) {
            advance(); // Consume 'not'.
            return new UnaryExprNode("not", parseUnaryExpr()); // Recursive call allows chaining.
        }

        return parsePostfixExpr(); // If no unary operator, parse the next stronger level.
    }

    // Parses postfix expressions: calls, indexing, and field access.
    private ExprNode parsePostfixExpr() throws IOException {
        ExprNode expr = parsePrimaryExpr(); // Start from a basic expression.

        while (true) {
            if (check(SymbolType.LPAREN)) {
                advance(); // '('
                List<ExprNode> args = new ArrayList<>(); // Store call arguments.

                if (!check(SymbolType.RPAREN)) {
                    args.add(parseExpr()); // Parse first argument.
                    while (check(SymbolType.COMMA)) {
                        advance(); // Consume ',' between arguments.
                        args.add(parseExpr()); // Parse next argument.
                    }
                }

                match(SymbolType.RPAREN); // End of argument list.
                expr = new CallNode(expr, args); // Build function call node.
            } else if (check(SymbolType.LBRACKET)) {
                advance(); // '['
                ExprNode index = parseExpr(); // Parse index expression.
                match(SymbolType.RBRACKET); // Expect ']'.
                expr = new IndexAccessNode(expr, index); // Build array/index access.
            } else if (check(SymbolType.DOT)) {
                advance(); // '.'
                String fieldName = match(SymbolType.IDENTIFIER).getValue(); // Parse field name after dot.
                expr = new FieldAccessNode(expr, fieldName); // Build field access node.
            } else {
                break; // No more postfix operators.
            }
        }

        return expr;
    }

    private boolean isStartOfArrayConstructor() throws IOException {
        if (current.getType() == SymbolType.KEYWORD) {
            String v = current.getValue();
            if ("INT".equals(v) || "FLOAT".equals(v) || "STRING".equals(v) || "BOOL".equals(v) || "BOOLEAN".equals(v)) {
                return true;
            }
        }
        return current.getType() == SymbolType.COLLECTION_NAME;
    }

    // Parses the simplest expressions: literals, identifiers, array constructors, and parenthesized expressions.
    private ExprNode parsePrimaryExpr() throws IOException {
        // Parse array constructor expressions such as INT ARRAY [5] or Person ARRAY [n]
        if (isStartOfArrayConstructor()) {
            String type = parseTypeName(); // Parse the base type on the left.

            if (checkKeyword("ARRAY")) {
                advance(); // Consume 'ARRAY'.
                match(SymbolType.LBRACKET); // Expect '['.
                ExprNode size = parseExpr(); // Parse the array size expression.
                match(SymbolType.RBRACKET); // Expect ']'.

                List<ExprNode> args = new ArrayList<>();
                args.add(size);
                return new CallNode(new IdentifierNode(type + " ARRAY"), args);
            }

            return new IdentifierNode(type);
        }

        if (check(SymbolType.INT)) {
            String value = current.getValue(); // Read integer text.
            advance(); // Consume integer token.
            return new LiteralNode("Integer", value);
        }

        if (check(SymbolType.FLOAT)) {
            String value = current.getValue(); // Read float text.
            advance(); // Consume float token.
            return new LiteralNode("Float", value);
        }

        if (check(SymbolType.STRING)) {
            String value = current.getValue(); // Read string text.
            advance(); // Consume string token.
            return new LiteralNode("String", value);
        }

        if (check(SymbolType.BOOLEAN)) {
            String value = current.getValue(); // Read boolean text.
            advance(); // Consume boolean token.
            return new LiteralNode("Boolean", value);
        }

        if (check(SymbolType.IDENTIFIER) || check(SymbolType.COLLECTION_NAME)) {
            String name = current.getValue(); // Read identifier or collection name.
            advance(); // Consume identifier token.
            return new IdentifierNode(name);
        }

        if (check(SymbolType.LPAREN)) {
            advance(); // Consume '('.
            ExprNode expr = parseExpr(); // Parse expression inside parentheses.
            match(SymbolType.RPAREN); // Consume ')'.
            return expr; // Parentheses do not create a special AST node here.
        }

        throw new RuntimeException("Expected primary expression but got " + current); // Not a valid basic expression.
    }

    // Parses a return statement, with or without a returned expression.
    private ReturnNode parseReturnStmt() throws IOException {
        matchKeyword("return"); // Consume 'return'.

        if (check(SymbolType.SEMICOLON)) {
            return new ReturnNode(null); // "return;" without value.
        }

        ExprNode expr = parseExpr(); // Parse returned value.
        return new ReturnNode(expr);
    }

    // Parses an if statement, with optional else branch.
    private IfNode parseIfStmt() throws IOException {
        matchKeyword("if"); // Consume 'if'.
        match(SymbolType.LPAREN); // Expect '(' after if.
        ExprNode condition = parseExpr(); // Parse condition.
        match(SymbolType.RPAREN); // Expect ')'.

        StatementNode thenBranch = parseStatement(); // Parse then-part.
        StatementNode elseBranch = null;

        if (checkKeyword("else")) {
            advance(); // Consume 'else'.
            elseBranch = parseStatement(); // Parse else-part.
        }

        return new IfNode(condition, thenBranch, elseBranch);
    }

    // Parses a while loop.
    private WhileNode parseWhileStmt() throws IOException {
        matchKeyword("while"); // Consume 'while'.
        match(SymbolType.LPAREN); // Expect '('.
        ExprNode condition = parseExpr(); // Parse loop condition.
        match(SymbolType.RPAREN); // Expect ')'.

        StatementNode body = parseStatement(); // Parse loop body.
        return new WhileNode(condition, body);
    }

    // Parses a function definition with optional return type, parameters and a block body.
    private FunctionDefNode parseFunctionDef() throws IOException {
        matchKeyword("def"); // Consume 'def'.

        String returnType = null;
        String name;

        if (isStartOfVarDecl()) {
            returnType = parseTypeName(); // Parse return type if present.
            name = match(SymbolType.IDENTIFIER).getValue(); // Parse function name.
        } else {
            name = match(SymbolType.IDENTIFIER).getValue(); // Parse function name directly.
        }

        match(SymbolType.LPAREN); // Expect '('.

        List<ParamNode> params = new ArrayList<>();
        if (!check(SymbolType.RPAREN)) {
            params.add(parseParam()); // Parse first parameter.
            while (check(SymbolType.COMMA)) {
                advance(); // Consume ',' between parameters.
                params.add(parseParam()); // Parse next parameter.
            }
        }

        match(SymbolType.RPAREN); // End of parameter list.
        BlockNode body = parseBlock(); // Parse function body.

        return new FunctionDefNode(returnType, name, params, body);
    }

    // Parses a single function parameter: type + name.
    private ParamNode parseParam() throws IOException {
        String type = parseTypeName(); // Parse parameter type.
        String name = match(SymbolType.IDENTIFIER).getValue(); // Parse parameter name.
        return new ParamNode(type, name);
    }

    // Parses a for loop with init, condition, update, and body.
    private ForNode parseForStmt() throws IOException {
        matchKeyword("for"); // Consume 'for'.
        match(SymbolType.LPAREN); // Expect '('.

        // INIT
        ASTNode init = null;
        if (!check(SymbolType.SEMICOLON)) {
            if (isStartOfVarDecl()) {
                init = parseVarDecl(); // Init can be a declaration.
            } else {
                init = parseAssignmentOrExpr(); // Or an assignment / expression.
            }
        }
        match(SymbolType.SEMICOLON); // End of init part.

        // CONDITION
        ExprNode condition = null;
        if (!check(SymbolType.SEMICOLON)) {
            condition = parseExpr(); // Condition is an expression.
        }
        match(SymbolType.SEMICOLON); // End of condition part.

        // UPDATE
        ASTNode update = null;
        if (!check(SymbolType.RPAREN)) {
            update = parseAssignmentOrExpr(); // Update can be an assignment / expression.
        }

        match(SymbolType.RPAREN); // End of for header.

        StatementNode body = parseStatement(); // Parse loop body.
        return new ForNode(init, condition, update, body);
    }

    // Parses either a declaration, an assignment, or a normal expression.
    private ASTNode parseAssignmentOrExpr() throws IOException {
        if (isStartOfVarDecl()) {
            return parseVarDecl();
        }

        ExprNode left = parseExpr();

        if (check(SymbolType.ASSIGNMENTOPERATOR)) {
            if (!isAssignable(left)) {
                throw new RuntimeException("Invalid assignment target");
            }

            advance();
            ExprNode value = parseExpr();
            return new AssignmentNode(left, value);
        }

        return left;
    }

    // Parses a collection declaration: coll Name { fields... }.
    private CollDeclNode parseCollDecl() throws IOException {
        matchKeyword("coll"); // Consume 'coll'.

        String name = match(SymbolType.COLLECTION_NAME).getValue(); // Parse collection name.

        CollDeclNode coll = new CollDeclNode(name);

        match(SymbolType.LBRACE); // Start of field block.

        while (!check(SymbolType.RBRACE)) {
            String type = parseTypeName(); // Parse field type.
            String fieldName = match(SymbolType.IDENTIFIER).getValue(); // Parse field name.
            match(SymbolType.SEMICOLON); // Each field ends with ';'.

            coll.addField(new FieldNode(type, fieldName)); // Add field to collection node.
        }

        match(SymbolType.RBRACE); // End of collection block.

        return coll;
    }
}