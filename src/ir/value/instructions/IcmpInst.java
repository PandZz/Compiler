package ir.value.instructions;

import ir.type.IntType;
import ir.type.Type;
import ir.value.Value;

import java.util.HashMap;
import java.util.Map;

public class IcmpInst extends Instruction {
    private static final Map<Operator, String> opMap = new HashMap<>() {{
        put(Operator.Eq, "eq");
        put(Operator.Ne, "ne");
        put(Operator.Gt, "sgt");
        put(Operator.Ge, "sge");
        put(Operator.Lt, "slt");
        put(Operator.Le, "sle");
    }};

    public IcmpInst(Operator op, Value lhs, Value rhs) {
        super(IntType.i1, op);
        addOperand(lhs);
        addOperand(rhs);
        setName("%" + ++valNumber);
    }

    public Value getLHS() {
        return getOperand(0);
    }

    public Value getRHS() {
        return getOperand(1);
    }

    @Override
    public String toString() {
        return getName() + " = icmp " + opMap.get(getOp()) + " " +
                getLHS().getType() + " " +
                getLHS().getName() + ", " +
                getRHS().getName();
    }
}
