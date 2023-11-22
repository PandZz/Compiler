package ir.value;

import ir.type.LabelType;
import ir.value.instructions.Instruction;

public class BasicBlock extends User {
    public BasicBlock(Function function) {
        super(String.valueOf(++valNumber), new LabelType());
        function.addBasicBlock(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getName() + ":\n");
        Instruction inst;
        for (Value value : operands) {
            inst = (Instruction) value;
            sb.append(inst.toString()).append("\n");
        }
        return sb.toString();
    }
}
