package ir.value;

import ir.type.ArrayType;
import ir.type.Type;

import java.util.ArrayList;
import java.util.List;

public class ConstArray extends Const {
    private Type elementType;
    private List<Value> array;
    private int length;
    private boolean isZero;

    /**
     * 由于构造函数super的限制, 这里的type由外界事先求出再提供
     * @param type Type
     * @param array List<Value>
     */
    public ConstArray(Type type, List<Value> array) {
        super("", type);
        this.elementType = array.get(0).getType();
        this.array = new ArrayList<>(array);
        this.length = array.size();
        this.isZero = true;
        if (elementType instanceof ArrayType) {
            for (Value value : array) {
                if (!(value instanceof ConstArray) || !((ConstArray) value).isZero()) {
                    isZero = false;
                    break;
                }
            }
        } else {
            for (Value value : array) {
                if (!(value instanceof ConstInt) || !((ConstInt) value).isZero()) {
                    isZero = false;
                    break;
                }
            }
        }
    }

    public ConstArray(Type type) {
        super("", type);
        this.elementType = ((ArrayType) type).getElementType();
        this.array = new ArrayList<>();
        this.length = 0;
        this.isZero = true;
    }

    public boolean isZero() {
        return isZero;
    }

    public Type getElementType() {
        return elementType;
    }

    public List<Value> getArray() {
        return array;
    }

    public int getLength() {
        return length;
    }

    public Value getElement(List<Value> idxList) {
        List<Value> arr = this.array;
        for (Value idx : idxList) {
            if (idx instanceof ConstInt i) {
                Value ele = arr.get(i.getValue());
                if (ele instanceof ConstArray a) {
                    arr = a.getArray();
                } else
                    return ele instanceof ConstInt i32 ? i32 : null;
            } else {
                return null;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isZero) {
            sb.append(this.getType().toString()).append(" ").append("zeroinitializer");
        } else {
            sb.append(this.getType().toString()).append(" ").append("[");
            for (int i = 0; i < array.size(); i++) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(array.get(i).toString());
            }
            sb.append("]");
        }
        return sb.toString();
    }
}
