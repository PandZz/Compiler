package ir.value.instructions.terminator;

import ir.type.Type;
import ir.type.VoidType;
import ir.value.Value;
import ir.value.instructions.Operator;

public class BrInst extends TerminatorInst {
    private boolean isJmp;
    public BrInst(Value jmpBlock) {
        super(VoidType.voidType, Operator.Br);
        addOperand(jmpBlock);
        this.isJmp = true;
    }

    public BrInst(Value cond, Value trueBlock, Value falseBlock) {
        super(VoidType.voidType, Operator.Br);
        addOperand(cond);
        addOperand(trueBlock);
        addOperand(falseBlock);
        this.isJmp = false;
    }

    public Value getCond() {
        return getOperand(0);
    }

    public Value getTrueBlock() {
        return getOperand(1);
    }

    public Value getFalseBlock() {
        return getOperand(2);
    }

    public void setCond(Value cond) {
        setOperand(0, cond);
    }

    public void setTrueBlock(Value trueBlock) {
        setOperand(1, trueBlock);
    }

    public void setFalseBlock(Value falseBlock) {
        setOperand(2, falseBlock);
    }

    public boolean isJmp() {
        return isJmp;
    }

    @Override
    public String toString() {
        if (isJmp()) {
            return "br label %" + getOperand(0).getName();
        } else {
            return "br i1 " + getOperand(0).getName() + ", label %" + getOperand(1).getName() + ", label %" + getOperand(2).getName();
        }
    }
}
