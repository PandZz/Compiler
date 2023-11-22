package ir.type;

import java.util.ArrayList;
import java.util.List;

public class ArrayType implements Type {
    private final Type elementType;
    private int length;
    private final List<Integer> dimensions;

    public ArrayType(Type elementType) {
        this.elementType = elementType;
        this.dimensions = new ArrayList<>();
        this.length = 1;
    }

    public ArrayType(Type elementType, List<Integer> dimensions) {
        this.elementType = elementType;
        this.dimensions = dimensions;
        this.length = 1;
        for (int dimension : dimensions) {
            this.length *= dimension;
        }
    }

    public void addDimension(int dimension) {
        this.dimensions.add(dimension);
        this.length *= dimension;
    }

    public Type getElementType() {
        return elementType;
    }

    public int getLength() {
        return length;
    }

    public List<Integer> getDimensions() {
        return dimensions;
    }
}
