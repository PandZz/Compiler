package ir;

import ir.type.IntType;
import ir.type.Type;
import ir.type.VoidType;
import ir.value.BasicBlock;
import ir.value.ConstInt;
import ir.value.Function;
import ir.value.Value;
import ir.value.instructions.BinaryInst;
import ir.value.instructions.Operator;
import ir.value.instructions.terminator.RetInst;

import java.util.ArrayList;
import java.util.List;

public class IRBuildFactory {
    private static IRBuildFactory instance = null;
    private IRBuildFactory() {}
    public static IRBuildFactory getInstance() {
        if (instance == null)
            instance = new IRBuildFactory();
        return instance;
    }

    public Function createMainFunc() {
        return new Function("main", new IntType(32), new ArrayList<>());
    }

    public Function createFunction(String name, Type type, List<Type> parmTypes) {
        return new Function(name, type, parmTypes);
    }

    public BasicBlock createBasicBlock(Function function) {
        return new BasicBlock(function);
    }

    public RetInst createRetInst(BasicBlock basicBlock, Value retValue) {
        RetInst retInst = new RetInst(retValue);
        basicBlock.addOperand(retInst);
        return retInst;
    }

    public RetInst createRetInst(BasicBlock basicBlock) {
        RetInst retInst = new RetInst();
        basicBlock.addOperand(retInst);
        return retInst;
    }

    /**
     * 创建二元运算指令
     * @param op 运算符
     * @param lhs 左操作数
     * @param rhs 右操作数
     * @return 二元运算指令
     */
    public BinaryInst createBinaryInst(BasicBlock basicBlock, Operator op, Value lhs, Value rhs) {
        BinaryInst binaryInst = new BinaryInst(lhs.getType(), op, lhs, rhs);
        basicBlock.addOperand(binaryInst);
        return binaryInst;
    }

    public BinaryInst createNotInst(BasicBlock basicBlock, Value value) {
        return createBinaryInst(basicBlock, Operator.Eq, value, ConstInt.ZERO);
    }
}
