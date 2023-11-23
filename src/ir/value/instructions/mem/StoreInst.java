package ir.value.instructions.mem;

import ir.type.PointerType;
import ir.type.Type;
import ir.value.Value;
import ir.value.instructions.Operator;

public class StoreInst extends MemInst {
    public StoreInst(Value value, Value pointer) {
        super(value.getType(), Operator.Store);
        addOperand(value);
        addOperand(pointer);
    }

    public Value getLValue() {
        return getOperands().get(0);
    }

    public Value getPointer() {
        return getOperands().get(1);
    }

    @Override
    public String toString() {
        return "store " + getLValue().getType() + " " + getLValue().getName() + ", " + getPointer().getType() + " " + getPointer().getName();
    }
}
