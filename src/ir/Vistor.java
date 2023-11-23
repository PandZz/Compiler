package ir;

import ir.type.IntType;
import ir.type.Type;
import ir.type.VoidType;
import ir.value.*;
import ir.value.instructions.CallInst;
import ir.value.instructions.Operator;
import ir.value.instructions.mem.AllocaInst;
import ir.value.instructions.terminator.RetInst;
import node.NodeType;
import node.VNode;
import token.TokenType;
import utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Vistor {
    private static Vistor instance = null;
    private final IRBuildFactory factory;
    private List<Map<String, Value>> symTlbs;
    private List<Map<String, Integer>> constTlbs;
    private BasicBlock curBlk;
    private Function curFunc;
    private boolean isConstExp;

    /**
     * 当前的临时变量编号, 注意这个编号在每个函数中都是从0开始的, 且编号还会分配给基本块
     */
    private Vistor() {
        factory = IRBuildFactory.getInstance();
        symTlbs = new ArrayList<>();
        constTlbs = new ArrayList<>();
        curBlk = null;
        curFunc = null;
        isConstExp = false;
    }

    public static Vistor getInstance() {
        if (instance == null)
            instance = new Vistor();
        return instance;
    }

    private boolean isInGlobal() {
        return curFunc == null;
    }

    private void switchBlk() {
        curBlk = factory.createBasicBlock(curFunc);
    }

    private Value calc(Operator op, int l, int r) {
        switch (op) {
            case Add -> {
                return new ConstInt(IntType.i32, l + r);
            }
            case Sub -> {
                return new ConstInt(IntType.i32, l - r);
            }
            case Mul -> {
                return new ConstInt(IntType.i32, l * r);
            }
            case Div -> {
                return new ConstInt(IntType.i32, l / r);
            }
        }
        return null;
    }

    private Value calc(Operator op, int val) {
        switch (op) {
            case Not -> {
                return val == 0 ? ConstInt.ONE : ConstInt.ZERO;
            }
        }
        return null;
    }

    private Map<String, Value> getCurSymTbl() {
        return symTlbs.get(symTlbs.size() - 1);
    }

    private Map<String, Integer> getCurConstTbl() {
        return constTlbs.get(constTlbs.size() - 1);
    }

    /**
     * 递归地在符号表中寻找匹配的符号
     *
     * @param name 符号的名字
     * @return value: Value | null, 这里的Value可能是Function, GlobalVar, AllocaInst, Argument
     */
    private Value findSym(String name) {
        for (int i = symTlbs.size() - 1; i >= 0; i--) {
            if (symTlbs.get(i).containsKey(name))
                return symTlbs.get(i).get(name);
        }
        return null;
    }

    private Integer findConst(String name) {
        for (int i = constTlbs.size() - 1; i >= 0; i--) {
            if (constTlbs.get(i).containsKey(name))
                return constTlbs.get(i).get(name);
        }
        return null;
    }

    private void addSymbol(String name, Value value) {
        getCurSymTbl().put(name, value);
    }

    private void addConst(String name, Integer value) {
        getCurConstTbl().put(name, value);
    }

    private void pushSymTbl() {
        symTlbs.add(new HashMap<>());
    }

    private void pushConstTbl() {
        constTlbs.add(new HashMap<>());
    }

    private void pushTbl() {
        pushSymTbl();
        pushConstTbl();
    }

    private void popSymTbl() {
        symTlbs.remove(symTlbs.size() - 1);
    }

    private void popConstTbl() {
        constTlbs.remove(constTlbs.size() - 1);
    }

    private void popTbl() {
        popSymTbl();
        popConstTbl();
    }

    private final Map<String, TokenType> tokenTypeMap = new HashMap<>() {{
        for (TokenType tokenType : TokenType.values()) {
            put(tokenType.toString(), tokenType);
        }
    }};

    /**
     * 从语法树中的终结符节点获取其TokenType
     *
     * @param endNode 终结符
     * @return TokenType枚举
     */
    private TokenType getEndNodeTokenType(VNode endNode) {
        String s = endNode.getValue().split(" ")[0];
        return tokenTypeMap.getOrDefault(s, null);
    }

    /**
     * 从语法树中的终结符节点获取其值
     *
     * @param endNode 终结符
     * @return 形如";"或"a"的字符串
     */
    private String getEndNodeValue(VNode endNode) {
        String s = endNode.getValue();
        return s.substring(s.indexOf(" ") + 1);
    }

    // CompUnit → {Decl} {FuncDef} MainFuncDef
    public void visitCompUnit(VNode CompUnitNode) {
        pushTbl();
        List<Type> paramTypes = new ArrayList<>();
        addSymbol("getint", factory.createLibraryFunction("getint", IntType.i32, paramTypes));
        paramTypes.add(IntType.i32);
        addSymbol("putint", factory.createLibraryFunction("putint", VoidType.voidType, paramTypes));
        addSymbol("putch", factory.createLibraryFunction("putch", VoidType.voidType, paramTypes));
        for (VNode node : CompUnitNode.getChildrenNodes()) {
            switch (node.getNodeType()) {
                case Decl -> {
                    isConstExp = true;
                    visitDecl(node);
                    isConstExp = false;
                }
                case FuncDef -> visitFuncDef(node);
                case MainFuncDef -> visitMainFuncDef(node);
            }
        }
        popTbl();
    }

    // Decl → ConstDecl | VarDecl
    private void visitDecl(VNode declNode) {
        switch (declNode.get1stChildNode().getNodeType()) {
            // TODO: 这里有必要区分是全局变量还是局部变量(通过curFunc是否为null来判断)
            case ConstDecl -> visitConstDecl(declNode.get1stChildNode());
            case VarDecl -> visitVarDecl(declNode.get1stChildNode());
        }
    }

    // ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
    private void visitConstDecl(VNode constDeclNode) {
        VNode typeEndNode = constDeclNode.getChildNode(1).get1stChildNode();
        TokenType endNodeTokenType = getEndNodeTokenType(typeEndNode);
        Type type = null;
        if (endNodeTokenType == TokenType.INTTK) {
            type = IntType.i32;
        }
        for (VNode node : constDeclNode.getChildrenNodes()) {
            if (node.getNodeType() == NodeType.ConstDef) {
                visitConstDef(node, type);
            }
        }
    }

    // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
    private void visitConstDef(VNode constDefNode, Type type) {
        String name = getEndNodeValue(constDefNode.get1stChildNode());
        // TODO: 这里没有考虑数组
        Value value = visitConstInitVal(constDefNode.getLastChildNode(), type);
        if (isInGlobal()) {
            GlobalVar globalVar = factory.createGlobalVar(name, type, true, value);
            addSymbol(name, globalVar);
            addConst(name, ((ConstInt) value).getValue());
        } else {
            AllocaInst allocaInst = factory.createLocalVar(curBlk, value, type);
            addSymbol(name, allocaInst);
            if (isConstExp) {
                addConst(name, ((ConstInt) value).getValue());
            }
        }
    }

    // ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    private Value visitConstInitVal(VNode constInitValNode, Type type) {
        if (constInitValNode.getChildrenNodes().size() == 1) {
            // ConstInitVal → ConstExp
            return visitConstExp(constInitValNode.get1stChildNode());
        }
        // ConstInitVal → '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
        // TODO: 这里没有考虑数组
        return null;
    }

    // ConstExp → AddExp
    private Value visitConstExp(VNode constExpNode) {
        Value value = visitAddExp(constExpNode.get1stChildNode());
        return value;
    }

    // VarDecl → BType VarDef { ',' VarDef } ';'
    private void visitVarDecl(VNode varDeclNode) {
        VNode typeEndNode = varDeclNode.getChildNode(0).get1stChildNode();
        TokenType endNodeTokenType = getEndNodeTokenType(typeEndNode);
        Type type = null;
        if (endNodeTokenType == TokenType.INTTK) {
            type = IntType.i32;
        }
        for (VNode node : varDeclNode.getChildrenNodes()) {
            if (node.getNodeType() == NodeType.VarDef) {
                visitVarDef(node, type);
            }
        }
    }

    // VarDef → Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
    private void visitVarDef(VNode varDefNode, Type type) {
        String name = getEndNodeValue(varDefNode.get1stChildNode());
        // TODO: 这里没有考虑数组
        VNode lastChildNode = varDefNode.getLastChildNode();
        if (lastChildNode.getNodeType() == NodeType.InitVal) {
            // VarDef → Ident { '[' ConstExp ']' } '=' InitVal
            Value value = visitInitVal(lastChildNode, type);
            if (isInGlobal()) {
                GlobalVar globalVar = factory.createGlobalVar(name, type, false, value);
                addSymbol(name, globalVar);
                addConst(name, ((ConstInt) value).getValue());
            } else {
                AllocaInst allocaInst = factory.createLocalVar(curBlk, value, type);
                addSymbol(name, allocaInst);
                if (isConstExp) {
                    addConst(name, ((ConstInt) value).getValue());
                }
            }
        } else {
            // VarDef → Ident { '[' ConstExp ']' }
            if (isInGlobal()) {
                GlobalVar globalVar = factory.createGlobalVar(name, type, false, ConstInt.ZERO);
                addSymbol(name, globalVar);
            } else {
                AllocaInst allocaInst = factory.createLocalVar(curBlk, null, type);
                addSymbol(name, allocaInst);
            }
        }
    }

    // InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
    private Value visitInitVal(VNode initValNode, Type type) {
        if (initValNode.getChildrenNodes().size() == 1) {
            // InitVal → Exp
            return visitExp(initValNode.get1stChildNode());
        }
        // InitVal → '{' [ InitVal { ',' InitVal } ] '}'
        // TODO: 这里没有考虑数组
        return null;
    }

    // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
    private void visitFuncDef(VNode funcDefNode) {
        VNode typeEndNode = funcDefNode.getChildNode(0).get1stChildNode();
        TokenType endNodeTokenType = getEndNodeTokenType(typeEndNode);
        Type type = null;
        if (endNodeTokenType == TokenType.INTTK) {
            type = IntType.i32;
        } else if (endNodeTokenType == TokenType.VOIDTK) {
            type = VoidType.voidType;
        }
        String name = getEndNodeValue(funcDefNode.getChildNode(1));
        List<Type> paramTypes = new ArrayList<>();
        List<Pair<Type, String>> funcFParams = null;
        if (funcDefNode.getChildrenNodes().size() == 6) {
            // FuncDef → FuncType Ident '(' FuncFParams ')' Block
            VNode funcFParamsNode = funcDefNode.getChildNode(3);
            // 获取形参类型+名称列表
            funcFParams = visitFuncFParams(funcFParamsNode);
            // 生成形参类型列表
            for (Pair<Type, String> pair : funcFParams) {
                paramTypes.add(pair.getFirst());
            }
        }
        Value.valNumber = -1;
        Function func = factory.createFunction(name, type, paramTypes);
        curFunc = func;
        addSymbol(name, func);
        switchBlk();
        pushTbl();
        for (int i = 0; i < paramTypes.size(); i++) {
            Argument arg = func.getArgs().get(i);
            addSymbol(funcFParams.get(i).getSecond(), factory.createLocalVar(curBlk, arg, paramTypes.get(i)));
        }
        visitBlock(funcDefNode.getLastChildNode());
        popTbl();
    }

    // MainFuncDef → 'int' 'main' '(' ')' Block
    private void visitMainFuncDef(VNode mainFuncDefNode) {
        Function mainFunc = factory.createFunction("main", IntType.i32, new ArrayList<>());
        // TODO: 在每个函数进入时都要清空Value.valNumber, 并准备一个新的curBlk
        Value.valNumber = -1;
        curFunc = mainFunc;
        switchBlk();
        addSymbol("main", mainFunc);
        visitBlock(mainFuncDefNode.getLastChildNode());
    }

    // FuncFParams → FuncFParam { ',' FuncFParam }
    private List<Pair<Type, String>> visitFuncFParams(VNode funcFParamsNode) {
        List<Pair<Type, String>> funcFParams = new ArrayList<>();
        for (VNode node : funcFParamsNode.getChildrenNodes()) {
            if (node.getNodeType() == NodeType.FuncFParam) {
                funcFParams.add(visitFuncFParam(node));
            }
        }
        return funcFParams;
    }

    // FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
    private Pair<Type, String> visitFuncFParam(VNode funcFParamNode) {
        VNode typeEndNode = funcFParamNode.getChildNode(0).get1stChildNode();
        TokenType endNodeTokenType = getEndNodeTokenType(typeEndNode);
        Type type = null;
        if (endNodeTokenType == TokenType.INTTK) {
            type = IntType.i32;
        } else if (endNodeTokenType == TokenType.VOIDTK) {
            type = VoidType.voidType;
        }
        String name = getEndNodeValue(funcFParamNode.getChildNode(1));
        // TODO: 这里没有考虑数组
        return new Pair<>(type, name);
    }

    // Block → '{' { BlockItem } '}'
    private void visitBlock(VNode blockNode) {
        pushTbl();
        for (VNode node : blockNode.getChildrenNodes()) {
            if (node.getNodeType() == NodeType.BlockItem) {
                visitBlockItem(node);
            }
        }
        popTbl();
    }

    // BlockItem → Decl | Stmt
    private void visitBlockItem(VNode blockItemNode) {
        switch (blockItemNode.get1stChildNode().getNodeType()) {
            case Decl -> visitDecl(blockItemNode.get1stChildNode());
            case Stmt -> visitStmt(blockItemNode.get1stChildNode());
        }
    }

    // Stmt → LVal '=' Exp ';'
    //| [Exp] ';'
    //| Block
    //| 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    //| 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
    //| 'break' ';' | 'continue' ';'
    //| 'return' [Exp] ';'
    //| LVal '=' 'getint''('')'';'
    //| 'printf''('FormatString{','Exp}')'';'
    private void visitStmt(VNode stmtNode) {
        VNode firstChildNode = stmtNode.get1stChildNode();
        switch (firstChildNode.getNodeType()) {
            case LVal -> {
                if (stmtNode.getChildrenNodes().size() == 4) {
                    // LVal '=' Exp ';'
                    Value lVal = visitLVal(firstChildNode);
                    Value expValue = visitExp(stmtNode.getChildNode(2));
                    factory.createStoreInst(curBlk, lVal, expValue);
                } else {
                    // LVal '=' 'getint''('')'';'
                    Value lVal = visitLVal(firstChildNode);
                    Function getint = (Function) findSym("getint");
                    CallInst callInst = factory.createCallInst(curBlk, getint, new ArrayList<>());
                    factory.createStoreInst(curBlk, lVal, callInst);
                }
            }
            case Exp -> visitExp(firstChildNode);
            case Block -> visitBlock(firstChildNode);
            case EndNode -> {
                TokenType tokenType = getEndNodeTokenType(firstChildNode);
                switch (tokenType) {
//                    case IFTK -> visitIfStmt(stmtNode);
//                    case FORTK -> visitForStmt(stmtNode);
//                    case BREAKTK -> visitBreakStmt(stmtNode);
//                    case CONTINUETK -> visitContinueStmt(stmtNode);
                    case RETURNTK -> {
//                        visitExp(stmtNode.getChildNode(1));
                        RetInst retInst;
                        if (stmtNode.getChildrenNodes().size() == 3) {
                            retInst = factory.createRetInst(curBlk, visitExp(stmtNode.getChildNode(1)));
                        } else {
                            retInst = factory.createRetInst(curBlk);
                        }
                    }
                    case PRINTFTK -> {
                        // 'printf''('FormatString{','Exp}')'';'
                        // formatString: "abc%d\n"
                        String formatString = getEndNodeValue(stmtNode.getChildNode(2));
                        // 去掉首尾的双引号
                        formatString = formatString.substring(1, formatString.length() - 1);
                        List<Value> exps = new ArrayList<>();
                        for (VNode node : stmtNode.getChildrenNodes()) {
                            if (node.getNodeType() == NodeType.Exp) {
                                Value value = visitExp(node);
                                exps.add(value);
                            }
                        }
                        int j = 0;
                        Value value = null;
                        List<Value> args = new ArrayList<>();
                        for (int i = 0; i < formatString.length(); ++i) {
                            args.clear();
                            char c = formatString.charAt(i);
                            switch (c) {
                                case '\\' -> {
                                    ++i;
                                    c = formatString.charAt(i);
                                    switch (c) {
                                        case 'n' -> {
                                            value = new ConstInt(IntType.i32, 10);
                                            args.add(value);
                                        }
                                        case '\\' -> {
                                            value = new ConstInt(IntType.i32, 92);
                                            args.add(value);
                                        }
                                    }
                                    factory.createCallInst(curBlk, (Function) findSym("putch"), args);
                                }
                                case '%' -> {
                                    ++i;
                                    c = formatString.charAt(i);
                                    switch (c) {
                                        case 'd' -> {
                                            value = exps.get(j++);
                                            args.add(value);
                                            factory.createCallInst(curBlk, (Function) findSym("putint"), args);
                                        }
                                        case 'c' -> {
                                            value = exps.get(j++);
                                            args.add(value);
                                            factory.createCallInst(curBlk, (Function) findSym("putch"), args);
                                        }
                                    }
                                }
                                default -> {
                                    value = new ConstInt(IntType.i32, c);
                                    args.add(value);
                                    factory.createCallInst(curBlk, (Function) findSym("putch"), args);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Exp → AddExp
    private Value visitExp(VNode expNode) {
        Value value = visitAddExp(expNode.get1stChildNode());
        return value;
    }

    // AddExp → MulExp | AddExp ('+' | '−') MulExp
    private Value visitAddExp(VNode addExpNode) {
        if (addExpNode.getChildrenNodes().size() == 1) {
            return visitMulExp(addExpNode.get1stChildNode());
        }
        Value addExpValue = visitAddExp(addExpNode.get1stChildNode());
        Value mulExpValue = visitMulExp(addExpNode.getChildNode(2));
        VNode opNode = addExpNode.getChildNode(1);
        TokenType tokenType = getEndNodeTokenType(opNode);
        Operator op = null;
        switch (tokenType) {
            case PLUS -> op = Operator.Add;
            case MINU -> op = Operator.Sub;
        }
        return isConstExp ? calc(op, ((ConstInt) addExpValue).getValue(), ((ConstInt) mulExpValue).getValue()) : factory.createBinaryInst(curBlk, op, addExpValue, mulExpValue);
    }

    // MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    private Value visitMulExp(VNode mulExpNode) {
        if (mulExpNode.getChildrenNodes().size() == 1) {
            return visitUnaryExp(mulExpNode.get1stChildNode());
        }
        Value mulExpValue = visitMulExp(mulExpNode.get1stChildNode());
        Value unaryExpValue = visitUnaryExp(mulExpNode.getChildNode(2));
        VNode opNode = mulExpNode.getChildNode(1);
        TokenType tokenType = getEndNodeTokenType(opNode);
        Operator op = null;
        switch (tokenType) {
            case MULT -> op = Operator.Mul;
            case DIV -> op = Operator.Div;
            case MOD -> op = Operator.Mod;
        }
        return isConstExp ? calc(op, ((ConstInt) mulExpValue).getValue(), ((ConstInt) unaryExpValue).getValue()) : factory.createBinaryInst(curBlk, op, mulExpValue, unaryExpValue);
    }

    // UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    private Value visitUnaryExp(VNode unaryExpNode) {
        if (unaryExpNode.getChildrenNodes().size() == 1) {
            // UnaryExp → PrimaryExp
            return visitPrimaryExp(unaryExpNode.get1stChildNode());
        }
        if (unaryExpNode.getChildrenNodes().size() == 2) {
            // UnaryExp → UnaryOp UnaryExp
            VNode unaryOpNode = unaryExpNode.get1stChildNode();
            TokenType tokenType = getEndNodeTokenType(unaryOpNode.get1stChildNode());
            Operator op = null;
            Value value = visitUnaryExp(unaryExpNode.getChildNode(1));
            switch (tokenType) {
                case PLUS -> op = Operator.Add;
                case MINU -> op = Operator.Sub;
                case NOT -> {
                    op = Operator.Not;
                    return isConstExp ? calc(op, ((ConstInt) value).getValue()) : factory.createNotInst(curBlk, value);
                }
            }
            if (isConstExp) {
                assert op != null;
                return calc(op, 0, ((ConstInt) value).getValue());
            } else {
                return factory.createBinaryInst(curBlk, op, ConstInt.ZERO, value);
            }
        }
        // UnaryExp → Ident '(' [FuncRParams] ')'
        String funcName = getEndNodeValue(unaryExpNode.get1stChildNode());
        Function func = (Function) findSym(funcName);
        List<Value> funcRParams = new ArrayList<>();
        if (unaryExpNode.getChildrenNodes().size() == 4) {
            // UnaryExp → Ident '(' FuncRParams ')'
            VNode funcRParamsNode = unaryExpNode.getChildNode(2);
            funcRParams.addAll(visitFuncRParams(funcRParamsNode));
        }
        return factory.createCallInst(curBlk, func, funcRParams);
    }

    // FuncRParams → Exp { ',' Exp }
    private List<Value> visitFuncRParams(VNode funcRParamsNode) {
        List<Value> funcRParams = new ArrayList<>();
        for (VNode node : funcRParamsNode.getChildrenNodes()) {
            if (node.getNodeType() == NodeType.Exp) {
                funcRParams.add(visitExp(node));
            }
        }
        return funcRParams;
    }

    // PrimaryExp → '(' Exp ')' | LVal | Number
    private Value visitPrimaryExp(VNode primaryExpNode) {
        if (primaryExpNode.getChildrenNodes().size() == 1) {
            // PrimaryExp → LVal | Number
            VNode firstChildNode = primaryExpNode.get1stChildNode();
            switch (firstChildNode.getNodeType()) {
                case LVal -> {
                    Value value = visitLVal(firstChildNode);
                    if (isConstExp)
                        return value;
                    return factory.createLoadInst(curBlk, value);
                }
                case Number -> {
                    String numberStr = getEndNodeValue(firstChildNode.get1stChildNode());
                    return new ConstInt(IntType.i32, Integer.parseInt(numberStr));
                }
            }
        }
        // PrimaryExp → '(' Exp ')'
        return visitExp(primaryExpNode.getChildNode(1));
    }

    // LVal → Ident {'[' Exp ']'}
    private Value visitLVal(VNode lValNode) {
        String name = getEndNodeValue(lValNode.get1stChildNode());
        if (lValNode.getChildrenNodes().size() == 1) {
            // LVal → Ident
            if (isConstExp)
                return new ConstInt(IntType.i32, findConst(name));
            return findSym(name);
        }
        // LVal → Ident {'[' Exp ']'}
        // TODO: 这里没有考虑数组
        return null;
    }
}
