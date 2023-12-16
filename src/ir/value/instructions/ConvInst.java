package ir.value.instructions;

import ir.type.IntType;
import ir.type.PointerType;
import ir.type.VoidType;
import ir.value.Value;

public class ConvInst extends Instruction {
    public ConvInst(Operator op, Value value) {
        super(VoidType.voidType, op);
        if (op == Operator.Zext) {
            setType(IntType.i32);
        } else if (op == Operator.Bitcast) {
            setType(new PointerType(IntType.i32));
        } else if (op == Operator.Trunc) {
            setType(IntType.i1);
        }
        addOperand(value);
        setName("%" + ++valNumber);
    }

    @Override
    public String toString() {
        if (getOp() == Operator.Zext) {
            return getName() + " = zext i1 " + getOperands().get(0).getName() + " to i32";
        } else if (getOp() == Operator.Bitcast) {
            return getName() + " = bitcast " + getOperands().get(0).getType() + getOperands().get(0).getName() + " to i32*";
        } else if (getOp() == Operator.Trunc) {
            return getName() + " = trunc i32 " + getOperands().get(0).getName() + " to i1";
        }
        return "";
    }
}
