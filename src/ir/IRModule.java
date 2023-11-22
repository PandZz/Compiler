package ir;

import ir.value.Function;
import ir.value.GlobalVar;
import utils.IOUtils;

import java.util.ArrayList;
import java.util.List;

public class IRModule {
    private static IRModule instance;
    private IRModule() {
        globalVars = new ArrayList<>();
        functions = new ArrayList<>();
    }
    public static IRModule getInstance() {
        if (instance == null)
            instance = new IRModule();
        return instance;
    }
    private List<GlobalVar> globalVars;
    private List<Function> functions;

    public List<GlobalVar> getGlobalVars() {
        return globalVars;
    }

    public List<Function> getFunctions() {
        return functions;
    }

    public void addGlobalVar(GlobalVar globalVar) {
        globalVars.add(globalVar);
    }

    public void addFunction(Function function) {
        this.functions.add(function);
    }

    public void print2Buffer() {
        IOUtils.appendBuffer("declare i32 @getint()          ; 读取一个整数\n" +
                "declare void @putint(i32)      ; 输出一个整数\n" +
                "declare void @putch(i32)       ; 输出一个字符\n" +
                "declare void @putstr(i8*)      ; 输出字符串");
        for (GlobalVar globalVar : globalVars) {
            IOUtils.appendBuffer(globalVar.toString());
        }
        for (Function function : functions) {
            IOUtils.appendBuffer(function.toString());
        }
    }
}
