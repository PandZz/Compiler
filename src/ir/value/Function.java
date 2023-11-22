package ir.value;

import ir.IRModule;
import ir.type.FunctionType;
import ir.type.Type;

import java.util.ArrayList;
import java.util.List;

public class Function extends User {
    // TODO: 这里只考虑了main函数, 且其无参
    private List<Argument> args;
    private List<BasicBlock> basicBlocks;
    public Function(String name, Type returnType, List<Type> paramTypes) {
        super(name, new FunctionType(returnType, paramTypes));
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("define dso_local " + ((FunctionType) getType()).getReturnType().toString() + " @" + getName() + "(");
        for (int i = 0; i < args.size(); i++) {
            sb.append(args.get(i).getType().toString()).append(" ").append(args.get(i).getName());
            if (i != args.size() - 1)
                sb.append(", ");
        }
        sb.append(") {\n");
        for (BasicBlock basicBlock : basicBlocks) {
            sb.append(basicBlock.toString());
        }
        sb.append("}\n");
        return sb.toString();
    }
}
