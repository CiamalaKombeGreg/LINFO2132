package compiler;

import java.io.FileReader;
import java.io.Reader;

import compiler.Lexer.Lexer;
import compiler.Parser.ASTNode;
import compiler.Parser.Parser;

public class Compiler {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: -lexer <file> or -parser <file>");
            System.exit(1);
        }

        String mode = args[0];
        String path = args[1];

        try (Reader reader = new FileReader(path)) {
            switch (mode) {
                case "-lexer":
                    {
                        Lexer lexer = new Lexer(reader);
                        while (true) {
                            var s = lexer.getNextSymbol();
                            System.out.println(s);
                            if (s.getType().name().equals("EOF")) break;
                        }       break;
                    }
                case "-parser":
                    {
                        Lexer lexer = new Lexer(reader);
                        Parser parser = new Parser(lexer);
                        ASTNode ast = parser.getAST();
                        System.out.println(ast);
                        break;
                    }
                default:
                    System.err.println("Unknown mode: " + mode);
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}