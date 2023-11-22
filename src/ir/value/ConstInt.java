package ir.value;

import ir.type.IntType;
import ir.type.Type;

public class ConstInt extends Const {
    private int value;
    public static final ConstInt ZERO = new ConstInt(IntType.i32, 0);
    public ConstInt(Type type, int value) {
        super(String.valueOf(value), type);
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
