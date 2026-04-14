package compiler.Semantic;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FunctionType extends Type {
    private final List<Type> parameterTypes; // Store the types of parameters
    private final Type returnType; // Store the return type of the function

    public FunctionType(List<Type> parameterTypes, Type returnType) {
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
    }

    public List<Type> getParameterTypes() {
        return parameterTypes;
    }

    public Type getReturnType() {
        return returnType;
    }

    @Override
    public String getName() { // Build a function signature string representation
        String params = parameterTypes.stream()
                .map(Type::getName)
                .collect(Collectors.joining(", "));
        return "(" + params + ") -> " + returnType.getName();
    }

    @Override
    public boolean equals(Object o) { // Check if two FunctionType instances are equal based on their parameter and return types
        if (!(o instanceof FunctionType other)) return false;
        return Objects.equals(parameterTypes, other.parameterTypes)
                && Objects.equals(returnType, other.returnType);
    }

    @Override
    public int hashCode() { // Java convention for hashCode when equals is overridden
        return Objects.hash(parameterTypes, returnType);
    }
}