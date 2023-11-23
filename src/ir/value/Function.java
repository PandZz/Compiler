package ir.value;

import ir.IRModule;
import ir.type.FunctionType;
import ir.type.Type;

import java.util.ArrayList;
import java.util.List;

public class Function extends User {
    // TODO: 这里只考虑了main函数, 且其无参
    private final boolean isLibrary;
    private List<Argument> args;
    private List<BasicBlock> basicBlocks;

    public Function(String name, Type returnType, List<Type> paramTypes, boolean isLibrary) {
        super(name, new FunctionType(returnType, paramTypes));
        this.isLibrary = isLibrary;
        args = new ArrayList<>();
        for (Type type : paramTypes) {
            Argument arg = new Argument(type);
            args.add(arg);
            addOperand(arg);
        }
        basicBlocks = new ArrayList<>();
        IRModule.getInstance().addFunction(this);
    }

    public void addBasicBlock(BasicBlock basicBlock) {
        basicBlocks.add(basicBlock);
        addOperand(basicBlock);
    }

    public List<Argument> getArgs() {
        return args;
    }

    public List<BasicBlock> getBasicBlocks() {
        return basicBlocks;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isLibrary)
            sb.append("declare ");
        else
            sb.append("define dso_local ");
        sb.append(((FunctionType) getType()).getReturnType().toString())
                .append(" @")
                .append(getName())
                .append("(");
        for (int i = 0; i < args.size(); i++) {
            sb.append(args.get(i).getType().toString());
            if (!isLibrary)
                sb.append(" ").append(args.get(i).getName());
            if (i != args.size() - 1)
                sb.append(", ");
        }
        if (basicBlocks.isEmpty())
            sb.append(")");
        else {
            sb.append(") {\n");
            for (BasicBlock basicBlock : basicBlocks) {
                sb.append(basicBlock.toString());
            }
            sb.append("}\n");
        }
        return sb.toString();
    }
}
