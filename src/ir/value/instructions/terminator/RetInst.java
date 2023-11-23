package ir.value.instructions.terminator;

import ir.type.VoidType;
import ir.value.Value;
import ir.value.instructions.Operator;

public class RetInst extends TerminatorInst {
    public RetInst() {
        super(VoidType.voidType, Operator.Ret);
    }

    public RetInst(Value retValue) {
        super(retValue.getType(), Operator.Ret);
        addOperand(retValue);
    }

    public boolean isVoid() {
        return this.getOperands().isEmpty();
    }

    @Override
    public String toString() {
        if (getOperands().size() == 1) {
            return "ret " + getOperands().get(0).getType() + " " + getOperands().get(0).getName();
        } else {
            return "ret void";
        }
    }
}
