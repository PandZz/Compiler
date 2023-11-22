package ir.value.instructions;

import ir.type.Type;
import ir.value.User;

public abstract class Instruction extends User {
    private Operator op;
    public Instruction(Type type, Operator op) {
        // 这里的名称自动生成(因此其name只是中间结果的一个标识符, 而与源程序定义的变量无关)
        super("%" + (++valNumber), type);
        this.op = op;
    }

    public Operator getOp() {
        return op;
    }
}
