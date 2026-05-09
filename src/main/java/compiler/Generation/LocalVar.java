package compiler.Generation;

public class LocalVar {
    private final String name;
    private final String type;
    private final int slot;

    public LocalVar(String name, String type, int slot) {
        this.name = name;
        this.type = type;
        this.slot = slot;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public int getSlot() {
        return slot;
    }
}