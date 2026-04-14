package compiler;

import java.io.FileReader;
import java.io.Reader;

import compiler.Lexer.Lexer;
import compiler.Parser.ASTNode;
import compiler.Parser.Parser;
import compiler.Semantic.SemanticAnalyzer;

public class Compiler {
    public static void main(String[] args) {
        String mode;
        String path;

        switch (args.length) {
            case 1 -> {
                mode = "-semantic";
                path = args[0];
            }
            case 2 -> {
                mode = args[0];
                path = args[1];
            }
            default -> {
                System.err.println("Usage: <file> or -lexer <file> or -parser <file> or -semantic <file>");
                System.exit(1);
                return;
            }
        }

        try (Reader reader = new FileReader(path)) {
            switch (mode) {
                case "-lexer" -> {
                    Lexer lexer = new Lexer(reader);
                    while (true) {
                        var s = lexer.getNextSymbol();
                        System.out.println(s);
                        if (s.getType().name().equals("EOF")) break;
                    }
                }

                case "-parser" -> {
                    Lexer lexer = new Lexer(reader);
                    Parser parser = new Parser(lexer);
                    ASTNode ast = parser.getAST();
                    System.out.println(ast);
                }

                case "-semantic" -> {
                    Lexer lexer = new Lexer(reader);
                    Parser parser = new Parser(lexer);
                    ASTNode ast = parser.getAST();

                    SemanticAnalyzer analyzer = new SemanticAnalyzer();
                    analyzer.analyze(ast);

                    System.exit(0);
                }

                default -> {
                    System.err.println("Unknown mode: " + mode);
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}