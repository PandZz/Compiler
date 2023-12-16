package ir;

import ir.type.IntType;
import ir.type.Type;
import ir.type.VoidType;
import ir.value.*;
import ir.value.instructions.CallInst;
import ir.value.instructions.Operator;
import ir.value.instructions.mem.AllocaInst;
import ir.value.instructions.terminator.BrInst;
import node.NodeType;
import node.VNode;
import token.TokenType;
import utils.Pair;

import java.util.*;

public class Vistor {
    private static Vistor instance = null;
    private final IRBuildFactory factory;
    private List<Map<String, Value>> symTlbs;
    private List<Map<String, Integer>> constTlbs;
    private BasicBlock curBlk;

    /**
     * 需要回填的基本块或者基本块之间的符号|| &&
     * ||: 1, &&: 0, 不是符号就是-1
     */
//    private final List<Pair<BasicBlock, Integer>> ifRefillList = new ArrayList<>();
    /**
     * for中的break和continue序号回填的br
     * 数字编号表是0: break还是1: continue
     */
//    private final List<Pair<BrInst, Integer>> forRefillList = new ArrayList<>();
    private Function curFunc;
    private boolean isConstExp;
    private int inFor = 0;

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

    private String switchBlk() {
        curBlk = factory.createBasicBlock(curFunc);
        return curBlk.getName();
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
        if (Objects.requireNonNull(op) == Operator.Not) {
            return val == 0 ? ConstInt.ONE : ConstInt.ZERO;
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
        visitBlock(funcDefNode.getLastChildNode(), null);
        popTbl();
    }

    // MainFuncDef → 'int' 'main' '(' ')' Block
    private void visitMainFuncDef(VNode mainFuncDefNode) {
        Function mainFunc = factory.createFunction("main", IntType.i32, new ArrayList<>());
        // TODO: 在每个函数进入时都要清空Value.valNumber, 并准备一个新的curBlk
        Value.valNumber = -1;
        curFunc = mainFunc;
        addSymbol("main", mainFunc);
        switchBlk();
        pushTbl();
        visitBlock(mainFuncDefNode.getLastChildNode(), null);
        popTbl();
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
    private void visitBlock(VNode blockNode, List<Pair<BrInst, Integer>> forRefillList) {
        for (VNode node : blockNode.getChildrenNodes()) {
            if (node.getNodeType() == NodeType.BlockItem) {
                visitBlockItem(node, forRefillList);
            }
        }
    }

    // BlockItem → Decl | Stmt
    private void visitBlockItem(VNode blockItemNode, List<Pair<BrInst, Integer>> forRefillList) {
        switch (blockItemNode.get1stChildNode().getNodeType()) {
            case Decl -> visitDecl(blockItemNode.get1stChildNode());
            case Stmt -> visitStmt(blockItemNode.get1stChildNode(), forRefillList);
        }
    }

    private boolean isAndBlk(int i, List<Pair<BasicBlock, Integer>> ifRefillList) {
        return i > 0 && ifRefillList.get(i - 1).getSecond() == 0 || i < ifRefillList.size() - 1 && ifRefillList.get(i + 1).getSecond() == 0;
    }

    private void refillIf(BasicBlock curIfTrueBlk, BasicBlock curIfFalseBlk, BasicBlock curIfFinalBlk, List<Pair<BasicBlock, Integer>> ifRefillList) {
        int len = ifRefillList.size();
        ifRefillList.add(new Pair<>(null, 1));
        ifRefillList.add(new Pair<>(curIfFalseBlk, -1));
        for (int i = 0, j = 0; i < len; ++i) {
            Pair<BasicBlock, Integer> pair = ifRefillList.get(i);
            if (pair.getSecond() == -1) {
                if (isAndBlk(i, ifRefillList)) {
                    if (j <= i) {
                        j = i + 1;
                        while (ifRefillList.get(j).getSecond() != 1) {
                            ++j;
                        }
                        ++j;
                    }
                    BasicBlock tblk = ifRefillList.get(i + 2).getFirst();
                    BasicBlock fblk = ifRefillList.get(j).getFirst();
                    if (ifRefillList.get(i + 1).getSecond() == 1) {
                        tblk = curIfTrueBlk;
                    }
                    BasicBlock rblk = ifRefillList.get(i).getFirst();
                    BrInst brInst = rblk.getLastInst() instanceof BrInst ? (BrInst) rblk.getLastInst() : null;
                    if (brInst != null) {
                        brInst.setTrueBlock(tblk);
                        brInst.setFalseBlock(fblk);
                    }
//                    factory.createBrInst(rblk, rblk.getLastInst(), tblk, fblk);
                } else {
                    BasicBlock rblk = ifRefillList.get(i).getFirst();
                    BasicBlock tblk = curIfTrueBlk;
                    BasicBlock fblk = ifRefillList.get(i + 2).getFirst();
                    BrInst brInst = rblk.getLastInst() instanceof BrInst ? (BrInst) rblk.getLastInst() : null;
                    if (brInst != null) {
                        brInst.setTrueBlock(tblk);
                        brInst.setFalseBlock(fblk);
                    }
//                    factory.createBrInst(rblk, rblk.getLastInst(), tblk, fblk);
                }
            }
        }
//        factory.createBrInst(curIfTrueBlk, curIfFinalBlk);
    }

    // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    private void visitIfStmt(VNode stmtNode, List<Pair<BrInst, Integer>> forRefillList) {
        VNode condNode = stmtNode.getChildNode(2);
        VNode stmtTrueNode = stmtNode.getChildNode(4);
        List<Pair<BasicBlock, Integer>> ifRefillList = new ArrayList<>();
        BasicBlock curIfTrueBlk = null, curIfFalseBlk = null, curIfFinalBlk = null, lastTrueBlk = null, lastFalseBlk = null;
        if (stmtNode.getChildrenNodes().size() > 5) {
            // 'if' '(' Cond ')' Stmt 'else' Stmt
            VNode stmtFalseNode = stmtNode.getChildNode(6);

            visitCond(condNode, ifRefillList);

            switchBlk();
            curIfTrueBlk = curBlk;
            visitStmt(stmtTrueNode, forRefillList);
            lastTrueBlk = curBlk;

            switchBlk();
            curIfFalseBlk = curBlk;
            visitStmt(stmtFalseNode, forRefillList);
            lastFalseBlk = curBlk;

            switchBlk();
            curIfFinalBlk = curBlk;

            // refill trueBlk
            factory.createBrInst(lastTrueBlk, curIfFinalBlk);

            // refill falseBlk
            factory.createBrInst(lastFalseBlk, curIfFinalBlk);
        } else {
            // 'if' '(' Cond ')' Stmt
            /*
            * basicBlk
            * if (...)
            *   trueBlk
            * falseBlk(finalBlk)
            * -----------------
            * ...
            * br i1 <result>, label <trueBlk>, label <falseBlk>
            * */
            visitCond(condNode, ifRefillList);

            switchBlk();
            curIfTrueBlk = curBlk;
            visitStmt(stmtTrueNode, forRefillList);
            lastTrueBlk = curBlk;

            switchBlk();
            curIfFinalBlk = curBlk;

            curIfFalseBlk = curIfFinalBlk;

            // refill trueBlk
            factory.createBrInst(lastTrueBlk, curIfFinalBlk);
        }
        refillIf(curIfTrueBlk, curIfFalseBlk, curIfFinalBlk, ifRefillList);
    }

    private void refillFor(BasicBlock forStmt2Blk, BasicBlock finalBlk, List<Pair<BrInst, Integer>> forRefillList) {
        for (Pair<BrInst, Integer> pair : forRefillList) {
            if (pair.getSecond() == 0) {
                // break
                pair.getFirst().setJmpBlock(finalBlk);
            } else {
                // continue
                pair.getFirst().setJmpBlock(forStmt2Blk);
            }
        }
    }

    // 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
    private void visitFor(VNode stmtNode) {
        inFor++;
//        VNode forStmt1Node, condNode, forStmt2Node, forBlockNode;
        VNode[] nodes = new VNode[4];
        int idx = 0;
        for (VNode node : stmtNode.getChildrenNodes()) {
            if (node.getNodeType() == NodeType.EndNode) {
                TokenType type = getEndNodeTokenType(node);
                if (type == TokenType.SEMICN || type == TokenType.RPARENT) {
                    idx++;
                }
            } else {
                nodes[idx] = node;
            }
        }
        BasicBlock foreHeadBlk, condBlk = null, forStmt1Blk = null, forStmt2Blk = null, forBlk = null, lastForBlk = null;

        foreHeadBlk = curBlk;

        // forStmt1, 无需特殊处理
        if (nodes[0] != null) {
            forStmt1Blk = curBlk;
            visitForStmt(nodes[0]);
        }

        List<Pair<BasicBlock, Integer>> ifRefillList = new ArrayList<>();
        // cond
        if (nodes[1] != null) {
            switchBlk();
            condBlk = curBlk;
            visitCond(nodes[1], ifRefillList);
        }
        // TODOn: 若没有cond, 则默认为true, 可直接跳转到forBlock, 即condBlk = forBlk

        // forBlock
        // 没有cond(此时curBlk包含有指令, 并不是visitCond新创建的基本块), 才需要switchBlk
        switchBlk();
        forBlk = curBlk;
        List<Pair<BrInst, Integer>> forRefillList = new ArrayList<>();
        visitStmt(nodes[3], forRefillList);
        lastForBlk = curBlk;

        // forStmt2
        if (nodes[2] != null) {
            // 当前基本块不是新块才需要切换基本块
            switchBlk();
            forStmt2Blk = curBlk;
            visitForStmt(nodes[2]);
        }

        // refill forStmt1
        if (forStmt1Blk != null) {
            if (condBlk != null)
                factory.createBrInst(forStmt1Blk, condBlk);
            else factory.createBrInst(forStmt1Blk, forBlk);
        } else {
            // forStmt1不存在就从之前的blk跳入for
            if (condBlk != null)
                factory.createBrInst(foreHeadBlk, condBlk);
            else factory.createBrInst(foreHeadBlk, forBlk);
        }

        // get finalBlk
        switchBlk();
        BasicBlock curFinalBlk = curBlk;

        // refill cond
        if (condBlk != null) {
//            factory.createBrInst(condBlk, forBlk);
            refillIf(forBlk, curFinalBlk, curFinalBlk, ifRefillList);
        }

        // refill forBlk
        if (forStmt2Blk != null) {
            factory.createBrInst(lastForBlk, forStmt2Blk);
        } else if (condBlk != null) {
            factory.createBrInst(lastForBlk, condBlk);
        } else {
            factory.createBrInst(lastForBlk, forBlk);
        }

        // refill forStmt2
        if (forStmt2Blk != null){
            if (condBlk != null ){
                factory.createBrInst(forStmt2Blk, condBlk);
            } else {
                factory.createBrInst(forStmt2Blk, forBlk);
            }
        }

        // refill break and continue
        refillFor(forStmt2Blk, curFinalBlk, forRefillList);

        inFor--;
    }

    // 'printf''('FormatString{','Exp}')'';'
    private void visitPrintfStmt(VNode stmtNode) {
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

    // Stmt → LVal '=' Exp ';'
    //| [Exp] ';'
    //| Block
    //| 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
    //| 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
    //| 'break' ';' | 'continue' ';'
    //| 'return' [Exp] ';'
    //| LVal '=' 'getint''('')'';'
    //| 'printf''('FormatString{','Exp}')'';'
    private void visitStmt(VNode stmtNode, List<Pair<BrInst, Integer>> forRefillList) {
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
            case Block -> {
                pushTbl();
                visitBlock(firstChildNode, forRefillList);
                popTbl();
            }
            case EndNode -> {
                TokenType tokenType = getEndNodeTokenType(firstChildNode);
                switch (tokenType) {
                    case IFTK -> visitIfStmt(stmtNode, forRefillList);
                    case FORTK -> visitFor(stmtNode);
                    case BREAKTK -> {
                        if (inFor > 0) {
                            BrInst brInst = factory.createBrInst(curBlk, BasicBlock.PLACE_HOLDER);
                            forRefillList.add(new Pair<>(brInst, 0));
                            switchBlk();
                        }
                    }
                    case CONTINUETK -> {
                        if (inFor > 0) {
                            BrInst brInst = factory.createBrInst(curBlk, BasicBlock.PLACE_HOLDER);
                            forRefillList.add(new Pair<>(brInst, 1));
                            switchBlk();
                        }
                    }
                    case RETURNTK -> {
                        if (stmtNode.getChildrenNodes().size() == 3) {
                            factory.createRetInst(curBlk, visitExp(stmtNode.getChildNode(1)));
                        } else {
                            factory.createRetInst(curBlk);
                        }
                    }
                    case PRINTFTK -> visitPrintfStmt(stmtNode);
                }
            }
        }
    }

    // ForStmt → LVal '=' Exp
    private Value visitForStmt(VNode forStmtNode) {
        Value lVal = visitLVal(forStmtNode.get1stChildNode());
        Value expValue = visitExp(forStmtNode.getChildNode(2));
        return factory.createStoreInst(curBlk, lVal, expValue);
    }

    // Exp → AddExp
    private Value visitExp(VNode expNode) {
        return visitAddExp(expNode.get1stChildNode());
    }

    // Cond → LOrExp
    private Value visitCond(VNode condNode, List<Pair<BasicBlock, Integer>> ifRefillList) {
//        switchBlk();
        ifRefillList.add(new Pair<>(curBlk, -1));
        return visitLOrExp(condNode.get1stChildNode(), ifRefillList);
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

    // RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    private Value visitRelExp(VNode relExpNode) {
        if (relExpNode.getChildrenNodes().size() == 1) {
            return visitAddExp(relExpNode.get1stChildNode());
        }
        Value relExpValue = visitRelExp(relExpNode.get1stChildNode());
        Value addExpValue = visitAddExp(relExpNode.getChildNode(2));
        VNode opNode = relExpNode.getChildNode(1);
        TokenType tokenType = getEndNodeTokenType(opNode);
        Operator op = null;
        switch (tokenType) {
            case LSS -> op = Operator.Lt;
            case LEQ -> op = Operator.Le;
            case GRE -> op = Operator.Gt;
            case GEQ -> op = Operator.Ge;
        }
        return factory.createIcmpInst(curBlk, op, relExpValue, addExpValue);
    }

    // EqExp → RelExp | EqExp ('==' | '!=') RelExp
    private Value visitEqExp(VNode eqExpNode) {
        if (eqExpNode.getChildrenNodes().size() == 1) {
            return visitRelExp(eqExpNode.get1stChildNode());
        }
        Value eqExpValue = visitEqExp(eqExpNode.get1stChildNode());
        Value relExpValue = visitRelExp(eqExpNode.getChildNode(2));
        VNode opNode = eqExpNode.getChildNode(1);
        TokenType tokenType = getEndNodeTokenType(opNode);
        Operator op = null;
        switch (tokenType) {
            case EQL -> op = Operator.Eq;
            case NEQ -> op = Operator.Ne;
        }
        return factory.createIcmpInst(curBlk, op, eqExpValue, relExpValue);
    }

    // LAndExp → EqExp | LAndExp '&&' EqExp
    private Value visitLAndExp(VNode lAndExpNode, List<Pair<BasicBlock, Integer>> ifRefillList) {
        // LAndExp → EqExp
        if (lAndExpNode.getChildrenNodes().size() == 1) {
            Value res = visitEqExp(lAndExpNode.get1stChildNode());
            if (!(res instanceof BrInst)) {
                res = factory.createBrInst(curBlk, res, BasicBlock.PLACE_HOLDER, BasicBlock.PLACE_HOLDER);
            }
            return res;
        }
        // LAndExp → LAndExp '&&' EqExp
        visitLAndExp(lAndExpNode.get1stChildNode(), ifRefillList);
        ifRefillList.add(new Pair<>(null, 0));
        switchBlk();
        ifRefillList.add(new Pair<>(curBlk, -1));
        Value res = visitEqExp(lAndExpNode.getChildNode(2));
        if (!(res instanceof BrInst)) {
            res = factory.createBrInst(curBlk, res, BasicBlock.PLACE_HOLDER, BasicBlock.PLACE_HOLDER);
        }
        return res;
    }

    // LOrExp → LAndExp | LOrExp '||' LAndExp
    private Value visitLOrExp(VNode lOrExpNode, List<Pair<BasicBlock, Integer>> ifRefillList) {
        // LOrExp → LAndExp
        if (lOrExpNode.getChildrenNodes().size() == 1) {
            Value res = visitLAndExp(lOrExpNode.get1stChildNode(), ifRefillList);
            if (!(res instanceof BrInst)) {
                res = factory.createBrInst(curBlk, res, BasicBlock.PLACE_HOLDER, BasicBlock.PLACE_HOLDER);
            }
            return res;
        }
        // LOrExp → LOrExp '||' LAndExp
        visitLOrExp(lOrExpNode.get1stChildNode(), ifRefillList);
        ifRefillList.add(new Pair<>(null, 1));
        switchBlk();
        ifRefillList.add(new Pair<>(curBlk, -1));
        Value res = visitLAndExp(lOrExpNode.getChildNode(2), ifRefillList);
        if (!(res instanceof BrInst)) {
            res = factory.createBrInst(curBlk, res, BasicBlock.PLACE_HOLDER, BasicBlock.PLACE_HOLDER);
        }
        return res;
    }
}
