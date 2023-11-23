package ir.value.instructions;

import ir.type.Type;
import ir.value.Value;

import java.util.HashMap;
import java.util.Map;

public class BinaryInst extends Instruction {
    private static final Map<Operator, String> opMap = new HashMap<>() {{
        put(Operator.Add, "add");
        put(Operator.Sub, "sub");
        put(Operator.Mul, "mul");
        put(Operator.Div, "sdiv");
        put(Operator.Mod, "srem");
        put(Operator.Shl, "shl");
        put(Operator.Shr, "ashr");
        put(Operator.And, "and");
        put(Operator.Or, "or");
    }};
    public BinaryInst(Type type, Operator op, Value lhs, Value rhs) {
        super(type, op);
        addOperand(lhs);
        addOperand(rhs);
        // 这里的名称自动生成(因此其name只是中间结果的一个标识符, 而与源程序定义的变量名无关)
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
        return getName() + " = " + opMap.get(getOp()) + " " + getLHS().getType() + " " + getLHS().getName() + ", " + getRHS().getName();
    }
}
