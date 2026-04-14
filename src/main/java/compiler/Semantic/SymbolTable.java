package compiler.Semantic;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private final SymbolTable parent; // Each symbol table has a reference to its parent, allowing for nested scopes
    private final Map<String, SymbolInfo> entries = new HashMap<>(); // Store symbol information for the current scope

    public SymbolTable(SymbolTable parent) {
        this.parent = parent;
    }

    public SymbolTable getParent() {
        return parent;
    }

    public boolean containsInCurrentScope(String name) {
        return entries.containsKey(name);
    }

    public void define(SymbolInfo info) { // Add a new symbol to the current scope
        entries.put(info.getName(), info);
    }

    public SymbolInfo resolve(String name) { // Look up a symbol by name, checking the current scope first and then parent scopes if necessary, returning null if the symbol is not found
        SymbolInfo local = entries.get(name);
        if (local != null) return local;
        if (parent != null) return parent.resolve(name);
        return null;
    }
}