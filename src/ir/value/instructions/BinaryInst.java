package ir.value.instructions;

import ir.type.Type;
import ir.value.Value;

public class BinaryInst extends Instruction {
    public BinaryInst(Type type, Operator op, Value lhs, Value rhs) {
        // 这里的名称自动生成(因此其name只是中间结果的一个标识符, 而与源程序定义的变量名无关)
        super(type, op);
        addOperand(lhs);
        addOperand(rhs);
    }

    public Value getLHS() {
        return getOperand(0);
    }

    public Value getRHS() {
        return getOperand(1);
    }

    @Override
    public String toString() {
        return getName() + " = " + getOp().toString().toLowerCase() + " " + getLHS().getType() + " " + getLHS().getName() + ", " + getRHS().getName();
    }
}
