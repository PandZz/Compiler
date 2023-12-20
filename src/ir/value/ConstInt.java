package ir.value;

import ir.type.IntType;
import ir.type.Type;

public class ConstInt extends Const {
    private int value;
    public static final ConstInt ZERO = new ConstInt(IntType.i32, 0);
    public static final ConstInt ONE = new ConstInt(IntType.i32, 1);
    public ConstInt(Type type, int value) {
        super(String.valueOf(value), type);
        this.value = value;
    }

    public ConstInt(int value) {
        super(String.valueOf(value), IntType.i32);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public boolean isZero() {
        return value == 0;
    }

    @Override
    public String toString() {
        return "i32 " + this.value;
    }
}
