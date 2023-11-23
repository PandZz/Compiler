package ir.value.instructions;

import ir.type.Type;
import ir.value.User;

public abstract class Instruction extends User {
    private Operator op;
    public Instruction(Type type, Operator op) {
        super("", type);
        this.op = op;
    }

    public Operator getOp() {
        return op;
    }
}
