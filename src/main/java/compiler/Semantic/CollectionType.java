package compiler.Semantic;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class CollectionType extends Type { // To create a new collection type, we need to define its name and the fields it contains.
    private final String name;
    private final Map<String, Type> fields = new LinkedHashMap<>(); // To store field names and their corresponding types, I used a LinkedHashMap to maintain the order of field definitions.

    public CollectionType(String name) {
        this.name = name;
    }

    public void addField(String fieldName, Type fieldType) {
        fields.put(fieldName, fieldType);
    }

    public boolean hasField(String fieldName) {
        return fields.containsKey(fieldName);
    }

    public Type getFieldType(String fieldName) {
        return fields.get(fieldName);
    }

    public Map<String, Type> getFields() {
        return fields;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) { // To compare two collection types, we check if their names are the same. This was a design presented in the project description
        if (!(o instanceof CollectionType other)) return false;
        return Objects.equals(name, other.name);
    }

    @Override // Java convention for hashCode when equals is overridden
    public int hashCode() {
        return Objects.hash(name);
    }
}