package ir.value;

import ir.Use;
import ir.type.Type;

import java.util.ArrayList;
import java.util.List;

public class User extends Value {
    protected List<Value> operands;

    public User(String name, Type type) {
        super(name, type);
        this.operands = new ArrayList<>();
    }

    public List<Value> getOperands() {
        return operands;
    }

    public Value getOperand(int index) {
        return operands.get(index);
    }

    public void addOperand(Value operand) {
        this.operands.add(operand);
        Use use = new Use(operand, this, operands.size() - 1);
        if (operand != null) {
            operand.addUse(use);
        }
        this.addUse(use);
    }
}
