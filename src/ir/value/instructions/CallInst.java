package ir.value.instructions;

import ir.type.FunctionType;
import ir.type.Type;
import ir.type.VoidType;
import ir.value.Function;
import ir.value.Value;

import java.util.List;

public class CallInst extends Instruction {
    public CallInst(Function function, List<Value> args) {
        super(((FunctionType) function.getType()).getReturnType(), Operator.Call);
        if (!(((FunctionType) function.getType()).getReturnType() instanceof VoidType)) {
            this.setName("%" + ++valNumber);
        }
        addOperand(function);
        for (Value arg : args) {
            addOperand(arg);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!(getType() instanceof VoidType)) {
            sb.append(getName())
                    .append(" = ");
        }
        sb.append("call ")
                .append(getType().toString())
                .append(" @")
                .append(getOperands().get(0).getName())
                .append("(");
        for (int i = 1; i < getOperands().size(); i++) {
            sb.append(getOperands().get(i).getType().toString())
                    .append(" ")
                    .append(getOperands().get(i).getName());
            if (i != getOperands().size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}
