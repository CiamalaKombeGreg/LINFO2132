package compiler.Generation;

import java.util.HashMap;
import java.util.Map;

public class CodeGenContext {
    private final Map<String, LocalVar> locals = new HashMap<>();
    private int nextSlot;

    public CodeGenContext(int firstSlot) {
        this.nextSlot = firstSlot;
    }

    public LocalVar declareLocal(String name, String type) {
        LocalVar local = new LocalVar(name, type, nextSlot);
        locals.put(name, local);
        nextSlot++;
        return local;
    }

    public LocalVar resolveLocal(String name) {
        return locals.get(name);
    }
}