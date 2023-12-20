package ir.value.instructions.mem;

import ir.type.ArrayType;
import ir.type.PointerType;
import ir.type.Type;
import ir.value.GlobalVar;
import ir.value.Value;
import ir.value.instructions.Operator;

import java.util.List;

public class GEPInst extends MemInst {
    private Type elementType;
    private Value target;

    public GEPInst(Value pointer, List<Value> indexs) {
        super(new PointerType(getElementType(pointer, indexs)), Operator.GEP);
        setName("%" + ++valNumber);
        if (pointer instanceof GEPInst) {
            target = ((GEPInst) pointer).target;
        } else if (pointer instanceof AllocaInst) {
            target = pointer;
        } else if (pointer instanceof GlobalVar) {
            target = pointer;
        }
        this.addOperand(pointer);
        for (Value value : indexs) {
            this.addOperand(value);
        }
        this.elementType = getElementType(pointer, indexs);
    }

    public Value getPointer() {
        return getOperands().get(0);
    }

    private static Type getElementType(Value pointer, List<Value> indexs) {
        Type type = pointer.getType();
        for (Value ignored : indexs) {
            if (type instanceof ArrayType) {
                type = ((ArrayType) type).getElementType();
            } else if (type instanceof PointerType) {
                type = ((PointerType) type).getTargetType();
            } else {
                break;
            }
        }
        return type;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(getName()).append(" = getelementptr ");
        // 如果是字符串，需要加 inbounds
        if (getPointer().getType() instanceof PointerType && ((PointerType) getPointer().getType()).isString()) {
            s.append("inbounds ");
        }
        s.append(((PointerType) getPointer().getType()).getTargetType()).append(", ");
        for (int i = 0; i < getOperands().size(); i++) {
            if (i == 0) {
                s.append(getPointer().getType()).append(" ").append(getPointer().getName());
            } else {
                s.append(", ").append(getOperands().get(i).getType()).append(" ").append(getOperands().get(i).getName());
            }
        }
        return s.toString();
    }
}
