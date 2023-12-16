package ir;

import ir.type.IntType;
import ir.type.PointerType;
import ir.type.Type;
import ir.type.VoidType;
import ir.value.*;
import ir.value.instructions.*;
import ir.value.instructions.mem.AllocaInst;
import ir.value.instructions.mem.LoadInst;
import ir.value.instructions.mem.StoreInst;
import ir.value.instructions.terminator.BrInst;
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
        return new Function("main", new IntType(32), new ArrayList<>(), false);
    }

    public Function createFunction(String name, Type type, List<Type> parmTypes) {
        return new Function(name, type, parmTypes, false);
    }

    public Function createLibraryFunction(String name, Type type, List<Type> parmTypes) {
        return new Function(name, type, parmTypes, true);
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

    public CallInst createCallInst(BasicBlock basicBlock, Function function, List<Value> args) {
        CallInst callInst = new CallInst(function, args);
        basicBlock.addOperand(callInst);
        return callInst;
    }

    public IcmpInst createIcmpInst(BasicBlock basicBlock, Operator op, Value lhs, Value rhs) {
        if (lhs.getType() instanceof IntType && ((IntType) lhs.getType()).getBit() == 1) {
            lhs = new ConvInst(Operator.Zext, lhs);
            basicBlock.addOperand(lhs);
        }
        if (rhs.getType() instanceof IntType && ((IntType) rhs.getType()).getBit() == 1) {
            rhs = new ConvInst(Operator.Zext, rhs);
            basicBlock.addOperand(rhs);
        }
        IcmpInst icmpInst = new IcmpInst(op, lhs, rhs);
        basicBlock.addOperand(icmpInst);
        return icmpInst;
    }

    public BrInst createBrInst(BasicBlock basicBlock, Value cond, BasicBlock trueBlock, BasicBlock falseBlock) {
        if (!(cond.getType() instanceof IntType && ((IntType) cond.getType()).getBit() == 1)) {
            cond = new IcmpInst(Operator.Ne, cond, ConstInt.ZERO);
            basicBlock.addOperand(cond);
        }
        BrInst brInst = new BrInst(cond, trueBlock, falseBlock);
        basicBlock.addOperand(brInst);
        return brInst;
    }

    public BrInst createBrInst(BasicBlock basicBlock, BasicBlock jmpBlock) {
        BrInst brInst = new BrInst(jmpBlock);
        basicBlock.addOperand(brInst);
        return brInst;
    }
}
