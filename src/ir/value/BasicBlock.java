package ir.value;

import ir.Use;
import ir.type.LabelType;
import ir.value.instructions.Instruction;

import java.util.List;

public class BasicBlock extends User {
    public static final BasicBlock PLACE_HOLDER = new BasicBlock();
    private Function parent;
    private BasicBlock prev;
    private BasicBlock next;
    public BasicBlock(Function function) {
        super(String.valueOf(++valNumber), new LabelType());
        function.addBasicBlock(this);
        parent = function;
        prev = null;
        next = null;
    }
    private BasicBlock() {
        super("", new LabelType());
    }

    public Function getParent() {
        return parent;
    }

    /**
     * 获取当前基本块的前一个基本块
     * @return 前一个基本块(可能为null)
     */
    public BasicBlock getPrev() {
        return prev;
    }

    public void setPrev(BasicBlock prev) {
        this.prev = prev;
    }

    /**
     * 获取当前基本块的下一个基本块
     * @return 下一个基本块(可能为null)
     */
    public BasicBlock getNext() {
        return next;
    }

    public void setNext(BasicBlock next) {
        this.next = next;
    }

    public Instruction getInst(int index) {
        return (Instruction) getOperand(index);
    }

    public Instruction getLastInst() {
        return (Instruction) getOperands().get(getOperands().size() - 1);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getName() + ":\n");
        Instruction inst;
        for (Value value : operands) {
            inst = (Instruction) value;
            sb.append("  ").append(inst.toString()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BasicBlock blk) {
            return parent.equals(blk.parent) && getName().equals(blk.getName());
        }
        return false;
    }
}
