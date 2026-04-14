package compiler.Semantic;

import java.util.Objects;

public class PrimitiveType extends Type { // Class for primitive types like int, float, string, bool, void
    public static final PrimitiveType INT = new PrimitiveType("INT");
    public static final PrimitiveType FLOAT = new PrimitiveType("FLOAT");
    public static final PrimitiveType STRING = new PrimitiveType("STRING");
    public static final PrimitiveType BOOL = new PrimitiveType("BOOL");
    public static final PrimitiveType VOID = new PrimitiveType("VOID");

    private final String name;

    private PrimitiveType(String name) { // Private constructor to prevent external instantiation
        this.name = name;
    }

    @Override
    public String getName() { // We return the name of the primitive type for error messages and debugging
        return name;
    }

    @Override
    public boolean equals(Object o) { // We only compare the name of the type for equality
        if (!(o instanceof PrimitiveType other)) return false;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() { // Java convention for hashCode when equals is overridden
        return Objects.hash(name);
    }
}