package ir.value;

import ir.IRModule;
import ir.type.IntType;
import ir.type.PointerType;
import ir.type.Type;
import ir.type.VoidType;

public class GlobalVar extends User {
    private boolean isConst;
    private Value value;
    public GlobalVar(String name, Type type, boolean isConst, Value value) {
        super("@" + name, new PointerType(type));
        this.isConst = isConst;
        this.value = value == null ? new Value("", VoidType.voidType) : value;
        IRModule.getInstance().addGlobalVar(this);
    }

    public boolean isConst() {
        return isConst;
    }

    public Value getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getName() + " = dso_local " + (isConst ? "constant" : "global") + " " + value.toString();
    }
}
