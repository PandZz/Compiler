package ir.value.instructions.mem;

import ir.type.PointerType;
import ir.type.Type;
import ir.value.instructions.Operator;

public class AllocaInst extends MemInst {
    public AllocaInst(Type type) {
        super(new PointerType(type), Operator.Alloca);
        setName("%" + ++valNumber);
    }

    @Override
    public String toString() {
        return getName() + " = alloca " + ((PointerType) getType()).getTargetType();
    }
}
