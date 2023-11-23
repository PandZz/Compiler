package ir;

import ir.type.IntType;
import ir.type.PointerType;
import ir.type.Type;
import ir.type.VoidType;
import ir.value.*;
import ir.value.instructions.BinaryInst;
import ir.value.instructions.Operator;
import ir.value.instructions.mem.AllocaInst;
import ir.value.instructions.mem.LoadInst;
import ir.value.instructions.mem.StoreInst;
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

    /*
    只有全局变量/常量, 以及函数会自动在其构造函数内添加到IRModule中
    而基本块, 指令, 值都不会自动添加到IRModule中, 需要手动添加
     */

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

    public GlobalVar createGlobalVar(String name, Type type, boolean isConst, Value value) {
        return new GlobalVar(name, type, isConst, value);
    }

    public StoreInst createStoreInst(BasicBlock basicBlock, Value ptr, Value value) {
        StoreInst storeInst = new StoreInst(value, ptr);
        basicBlock.addOperand(storeInst);
        return storeInst;
    }

    /**
     * 创建局部变量
     * @param basicBlock 所处基本块
     * @param value 初始值(非必需, 可为null)
     * @param allocaType 变量类型
     * @return 局部变量 - AllocaInst
     */
    public AllocaInst createLocalVar(BasicBlock basicBlock, Value value, Type allocaType) {
        AllocaInst allocaInst = new AllocaInst(allocaType);
        basicBlock.addOperand(allocaInst);
        if (value != null) {
            createStoreInst(basicBlock, allocaInst, value);
        }
        return allocaInst;
    }

    public LoadInst createLoadInst(BasicBlock basicBlock, Value pointer) {
        LoadInst loadInst = new LoadInst(pointer);
        basicBlock.addOperand(loadInst);
        return loadInst;
    }
}
