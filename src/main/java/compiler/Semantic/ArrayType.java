package compiler.Semantic;

import java.util.Objects;

public class ArrayType extends Type { // To represent array types, e.g., int[], string[], etc.
    private final Type elementType; // For example, if it's an array of integers, elementType would be the integer type.

    public ArrayType(Type elementType) { // Constructor to initialize the element type of the array.
        this.elementType = elementType;
    }

    public Type getElementType() { // To get the type of elements stored in the array.
        return elementType;
    }

    @Override
    public String getName() {
        return "ARRAY[" + elementType.getName() + "]";
    }

    @Override
    public boolean equals(Object o) { // Two array types are considered equal if their element types are equal.
        if (!(o instanceof ArrayType other)) return false;
        return Objects.equals(elementType, other.elementType);
    }

    @Override
    public int hashCode() { // Java convention for hashCode when equals is overridden
        return Objects.hash(elementType);
    }
}