package ir.type;

import java.util.ArrayList;
import java.util.List;

public class FunctionType implements Type {
    private Type returnType;
    private List<Type> paramTypes;

    public FunctionType(Type returnType) {
        this.returnType = returnType;
        this.paramTypes = new ArrayList<>();
    }

    public FunctionType(Type returnType, List<Type> paramTypes) {
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Type> getParamTypes() {
        return paramTypes;
    }

    public void addParamType(Type type) {
        paramTypes.add(type);
    }
}
