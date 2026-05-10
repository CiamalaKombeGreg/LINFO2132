package compiler;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;

import compiler.Generation.CodeGenerator;
import compiler.Lexer.Lexer;
import compiler.Parser.ASTNode;
import compiler.Parser.Parser;
import compiler.Parser.ProgramNode;
import compiler.Semantic.SemanticAnalyzer;

public class Compiler {
    public static void main(String[] args) {
        String mode;
        String path;

        if (args.length == 1 || args.length == 3) {
            String sourcePath = args[0];
            String outputPath = "Main.class";

            if (args.length == 3) {
                if (!"-o".equals(args[1])) {
                    System.err.println("Usage: source_file -o target_file");
                    System.exit(1);
                    return;
                }
                outputPath = args[2];
            }

            try (Reader reader = new FileReader(sourcePath)) {
                Lexer lexer = new Lexer(reader);
                Parser parser = new Parser(lexer);
                ASTNode ast = parser.getAST();

                SemanticAnalyzer analyzer = new SemanticAnalyzer();
                analyzer.analyze(ast);

                if (!(ast instanceof ProgramNode program)) {
                    throw new RuntimeException("AST root is not a ProgramNode");
                }

                String className = new File(outputPath).getName().replace(".class", "");
                CodeGenerator generator = new CodeGenerator(className);
                generator.generate(program, outputPath);

                System.exit(0);
                return;
            } catch (Exception e) {
                System.err.println(e.getMessage());
                System.exit(1);
                return;
            }
        }

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