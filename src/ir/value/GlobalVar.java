package ir.value;

import ir.IRModule;
import ir.type.Type;

public class GlobalVar extends User {
    private boolean isConst;
    private Value value;
    public GlobalVar(String name, Type type, boolean isConst, Value value) {
        super("@" + name, type);
        this.isConst = isConst;
        this.value = value;
        IRModule.getInstance().addGlobalVar(this);
    }
}
