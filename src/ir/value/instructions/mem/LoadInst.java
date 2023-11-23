package ir.value.instructions.mem;

import ir.type.PointerType;
import ir.type.Type;
import ir.value.Value;
import ir.value.instructions.Operator;

public class LoadInst extends MemInst {
    public LoadInst(Value pointer) {
        super(((PointerType) pointer.getType()).getTargetType(), Operator.Load);
        // TODO: 数组类型
        setName("%" + ++valNumber);
        addOperand(pointer);
    }

    public Value getPointer() {
        return getOperands().get(0);
    }

    @Override
    public String toString() {
        return getName() + " = load " + getType() + ", " + getPointer().getType() + " " + getPointer().getName();
    }
}
