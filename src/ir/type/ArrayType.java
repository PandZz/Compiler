package ir.type;

import ir.value.ConstInt;
import ir.value.Value;

import java.util.ArrayList;
import java.util.List;

public class ArrayType implements Type {
    private final Type elementType;
    private final int length;

    public ArrayType(Type elementType) {
        this.elementType = elementType;
        this.length = 0;
    }

    public ArrayType(Type elementType, int length) {
        this.elementType = elementType;
        this.length = length;
    }

    public Type getElementType() {
        return elementType;
    }

    public boolean is1DArray() {
        return elementType instanceof IntType && ((IntType) elementType).isI32();
    }

    public boolean isHDArray() {
        return elementType instanceof ArrayType;
    }

    public boolean isString() {
        return elementType instanceof IntType && ((IntType) elementType).isI8();
    }

    public int getLength() {
        return length;
    }

    public List<Integer> getDimensions() {
        List<Integer> dimensions = new ArrayList<>();
        for (Type type = this; type instanceof ArrayType; type = ((ArrayType) type).getElementType()) {
            dimensions.add(((ArrayType) type).getLength());
        }
        return dimensions;
    }

    public int getCapacity() {
        int capacity = 1;
        for (int dimension : getDimensions()) {
            capacity *= dimension;
        }
        return capacity;
    }

    public List<Value> offset2Index(int offset) {
        List<Value> index = new ArrayList<>();
        Type type = this;
        while (type instanceof ArrayType) {
            index.add(new ConstInt(IntType.i32, offset / ((ArrayType) type).getCapacity()));
            offset %= ((ArrayType) type).getCapacity();
            type = ((ArrayType) type).getElementType();
        }
        index.add(new ConstInt(IntType.i32, offset));
        return index;
    }

    public int index2Offset(List<Integer> index) {
        int offset = 0, i = 0;
        Type type = this;
        offset += index.get(i++) * ((ArrayType) type).getCapacity();
        while (type instanceof ArrayType) {
            type = ((ArrayType) type).getElementType();
            if (type instanceof ArrayType) {
                offset += index.get(i++) * ((ArrayType) type).getCapacity();
            } else {
                offset += index.get(i++);
            }
        }
        return offset;
    }

    @Override
    public String toString() {
        return "[" + length + " x " + elementType.toString() + "]";
    }

}