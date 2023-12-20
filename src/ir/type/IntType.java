package ir.type;

public class IntType implements Type {
    public static final IntType i1 = new IntType(1);
    public static final IntType i8 = new IntType(8);
    public static final IntType i32 = new IntType(32);
    private final int bit;

    public IntType(int bit) {
        this.bit = bit;
    }

    public int getBit() {
        return bit;
    }

    public boolean isI1() {
        return bit == 1;
    }

    public boolean isI8() {
        return bit == 8;
    }

    public boolean isI32() {
        return bit == 32;
    }

    @Override
    public String toString() {
        return "i" + bit;
    }
}
