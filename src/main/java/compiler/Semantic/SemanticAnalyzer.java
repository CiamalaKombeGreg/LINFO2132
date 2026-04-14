package compiler.Semantic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import compiler.Parser.ASTNode;
import compiler.Parser.CollDeclNode;
import compiler.Parser.FunctionDefNode;
import compiler.Parser.ProgramNode;
import compiler.Parser.VarDeclNode;

public class SemanticAnalyzer {
    private final Map<String, Type> typeTable = new HashMap<>(); // Store the types defined in the program, including built-in and user-defined collections
    private final SymbolTable globalSymbols = new SymbolTable(null); // Store global variables and functions, null parent since it's the global scope

    public SemanticAnalyzer() {
        registerBuiltInTypes(); // Initialize the type table with built-in types like INT, FLOAT, STRING, BOOL, VOID
    }

    public void analyze(ASTNode root) {
        if (!(root instanceof ProgramNode program)) { // Verify that the root of the AST is a ProgramNode from the parser
            throw new SemanticException("ScopeError", "Root AST node must be a ProgramNode");
        }

        firstPass(program);
        // secondPass(program);   // TODO
    }

    private void registerBuiltInTypes() {
        typeTable.put("INT", PrimitiveType.INT);
        typeTable.put("FLOAT", PrimitiveType.FLOAT);
        typeTable.put("STRING", PrimitiveType.STRING);
        typeTable.put("BOOL", PrimitiveType.BOOL);
        typeTable.put("BOOLEAN", PrimitiveType.BOOL); // Parser tolerates BOOLEAN in some places, so we map it to BOOL
        typeTable.put("VOID", PrimitiveType.VOID);
    }

    private void firstPass(ProgramNode program) {
        /* 
         * The first pass of semantic analysis registers all collections, functions, and global variables in the type table and global symbol table.
         * This allows us to resolve types and symbols in the second pass when we analyze function bodies and expressions.
         */
        for (ASTNode node : program.getElements()) {
            if (node instanceof CollDeclNode coll) {
                registerCollection(coll);
            } else if (node instanceof FunctionDefNode fn) {
                registerFunction(fn);
            } else if (node instanceof VarDeclNode varDecl) {
                registerGlobalVar(varDecl);
            }
        }
    }

    private void registerCollection(CollDeclNode coll) {
        String name = coll.getName();

        if (typeTable.containsKey(name)) { // Check if the collection name already exists in the type table
            throw new SemanticException("CollectionError",
                    "Collection type '" + name + "' overwrites an existing type");
        }

        if (name.isEmpty() || !Character.isUpperCase(name.charAt(0))) { // Enforce the convention that collection names must start with a capital letter
            throw new SemanticException("CollectionError",
                    "Collection name must begin with a capital letter: " + name);
        }

        CollectionType collType = new CollectionType(name);

        for (compiler.Parser.FieldNode field : coll.getFields()) { // Process each field in the collection declaration
            if (collType.hasField(field.getName())) { // Reject duplicate field names within the same collection
                throw new SemanticException("CollectionError",
                        "Duplicate field '" + field.getName() + "' in collection '" + name + "'");
            }

            Type fieldType = resolveType(field.getType(), "CollectionError");
            collType.addField(field.getName(), fieldType);
        }

        typeTable.put(name, collType); // Register the new collection type in the type table
    }

    private void registerFunction(FunctionDefNode fn) {
        String name = fn.getName(); // Get the function name from the AST node

        if (globalSymbols.containsInCurrentScope(name)) { // Check if the function name already exists in the global symbol table
            throw new SemanticException("ScopeError",
                    "Duplicate global declaration of '" + name + "'");
        }

        Type returnType = fn.getReturnType() == null
                ? PrimitiveType.VOID
                : resolveType(fn.getReturnType(), "ReturnError"); // If the function has no return type specified, we treat it as VOID.

        List<Type> parameterTypes = new ArrayList<>();
        for (compiler.Parser.ParamNode param : fn.getParams()) { // Process each parameter in the function declaration
            parameterTypes.add(resolveType(param.getType(), "ArgumentError")); // If the parameter type is unknown, resolveType will throw an error with the "ArgumentError" keyword
        }

        FunctionType fnType = new FunctionType(parameterTypes, returnType);
        globalSymbols.define(new SymbolInfo(name, fnType, SymbolKind.FUNCTION, true)); // Register the function in the global symbol table. Functions are always final (cannot be reassigned) and have the kind FUNCTION.
    }

    private void registerGlobalVar(VarDeclNode varDecl) {
        String name = varDecl.getName();

        if (globalSymbols.containsInCurrentScope(name)) { // Check if the variable name already exists in the global symbol table. This prevents duplicate global variable declarations (and by extension, global variables that would shadow functions or collections)
            throw new SemanticException("ScopeError",
                    "Duplicate global declaration of '" + name + "'");
        }

        Type type = resolveType(varDecl.getType(), "TypeError"); // Resolve the variable's type. If the type is unknown, resolveType will throw an error with the "TypeError" keyword
        globalSymbols.define(new SymbolInfo(name, type, SymbolKind.VARIABLE, varDecl.isFinal())); // Add the variable to the global symbol table.
    }

    private Type resolveType(String typeName, String keyword) {
        if (typeName.startsWith("ARRAY[")) { // If the type name starts with "ARRAY[", we treat it as an array type.
            String inner = typeName.substring(6, typeName.length() - 1); // Extract the inner type name from the array type declaration
            return new ArrayType(resolveType(inner, keyword));
        }

        Type type = typeTable.get(typeName); // Look up the type name in the type table. This will find built-in types like INT, FLOAT, STRING, BOOL, VOID, as well as any user-defined collection types that were registered in the first pass.
        if (type == null) { // If the type is not found in the type table, we throw a semantic exception indicating that the type is unknown.
            throw new SemanticException(keyword, "Unknown type: " + typeName);
        }
        return type;
    }

    public Map<String, Type> getTypeTable() { // For testing and debugging purposes
        return typeTable;
    }

    public SymbolTable getGlobalSymbols() { // For testing and debugging purposes
        return globalSymbols;
    }
}