package ir;

import ir.type.*;
import ir.value.*;
import ir.value.instructions.*;
import ir.value.instructions.mem.AllocaInst;
import ir.value.instructions.mem.GEPInst;
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

    public IcmpInst createNotInst(BasicBlock basicBlock, Value value) {
        return createIcmpInst(basicBlock, Operator.Eq, value, ConstInt.ZERO);
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
//        if (jmpBlock == null)
//            throw new RuntimeException("jmpBlock is null");
        BrInst brInst = new BrInst(jmpBlock);
        basicBlock.addOperand(brInst);
        return brInst;
    }

    public ArrayType getArrayType(Type elementType, int length) {
        return new ArrayType(elementType, length);
    }

    public ConstArray createConstArray(List<Value> array) {
        Type arrType = getArrayType(array.get(0).getType(), array.size());
        return new ConstArray(arrType, array);
    }

    /**
     * 创建无显式初始值的全局数组
     * @param name
     * @param arrType
     * @param isConst
     * @return
     */
    public GlobalVar createGlobalArray(String name, Type arrType, boolean isConst) {
        ConstArray array = new ConstArray(arrType);
        return createGlobalVar(name, arrType, isConst, array);
    }

    /**
     * 创建有显式初始值的全局数组
     * @param name
     * @param arrType
     * @param arr 数组的显式初始值
     * @param isConst
     * @return
     */
    public GlobalVar createGlobalArray(String name, Type arrType, boolean isConst, List<Value> arr) {
        if (arr == null || arr.isEmpty())
            return createGlobalArray(name, arrType, isConst);
        ConstArray array = new ConstArray(arrType, arr);
        return createGlobalVar(name, arrType, isConst, array);
    }

    public GEPInst createGEPInst(BasicBlock basicBlock, Value pointer, List<Value> indices) {
        GEPInst gepInst = new GEPInst(pointer, indices);
        basicBlock.addOperand(gepInst);
        return gepInst;
    }

    public void initArray(BasicBlock blk, Value pointer, ConstInt idx, Value value) {
        if (((PointerType) pointer.getType()).getTargetType() instanceof IntType) {
            createStoreInst(blk, pointer, value);
        } else {
            ArrayList<Value> list = new ArrayList<>();
            list.add(ConstInt.ZERO);
//            list.add(idx);
//            createGEPInst(blk, pointer, list);
            for (int i = 0; i < ((ArrayType) ((PointerType) pointer.getType()).getTargetType()).getLength(); i++) {
                ConstInt nidx = new ConstInt(i);
                list.add(nidx);
                GEPInst ptr = createGEPInst(blk, pointer, list);
                list.remove(list.size() - 1);
                initArray(blk, ptr, nidx, ((ConstArray) value).getArray().get(i));
            }
        }
    }

    public AllocaInst createLocalArray(BasicBlock basicBlock, Type arrType, Value value) {
        AllocaInst allocaInst = new AllocaInst(arrType);
        // TODO: 为数组设置初始值
        basicBlock.addOperand(allocaInst);
        if (value != null) {
//            System.out.println(allocaInst);
//            System.out.println(value);
            initArray(basicBlock, allocaInst, ConstInt.ZERO, value);
        }
        return allocaInst;
    }
}
