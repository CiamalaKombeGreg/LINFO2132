package compiler.Semantic;

public abstract class Type { // Superclass for all types
    public abstract String getName();

    @Override
    public String toString() {
        return getName(); // We return the name of the type for easy debugging and error messages
    }
}