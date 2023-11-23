package ir;

import ir.type.IntType;
import ir.type.Type;
import ir.value.*;
import ir.value.instructions.Operator;
import ir.value.instructions.mem.AllocaInst;
import ir.value.instructions.terminator.RetInst;
import node.NodeType;
import node.VNode;
import token.TokenType;

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
     * @return value: Value | null
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
        return endNode.getValue().split(" ")[1];
    }

    // CompUnit → {Decl} {FuncDef} MainFuncDef
    public void visitCompUnit(VNode CompUnitNode) {
        pushTbl();
        for (VNode node : CompUnitNode.getChildrenNodes()) {
            switch (node.getNodeType()) {
                case Decl -> {
                    isConstExp = true;
                    visitDecl(node);
                    isConstExp = false;
                }
//                case FuncDef -> visitFuncDef(node);
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
        } else {
            // VarDef → Ident { '[' ConstExp ']' }
            if (isInGlobal()) {
                GlobalVar globalVar = factory.createGlobalVar(name, type, true, null);
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

    // Block → '{' { BlockItem } '}'
    private void visitBlock(VNode blockNode) {
        pushTbl();
        for (VNode node : blockNode.getChildrenNodes()) {
            switch (node.getNodeType()) {
                case BlockItem -> visitBlockItem(node);
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
                }
            }
        }
    }

    // Exp → AddExp
    private Value visitExp(VNode expNode) {
        Value value = visitAddExp(expNode.get1stChildNode());
        return value;
    }

    // AddExp → MulExp | MulExp ('+' | '−') AddExp
    private Value visitAddExp(VNode addExpNode) {
        Value mulExpValue = visitMulExp(addExpNode.get1stChildNode());
        if (addExpNode.getChildrenNodes().size() == 1) {
            return mulExpValue;
        }
        Value addExpValue = visitAddExp(addExpNode.getChildNode(2));
        VNode opNode = addExpNode.getChildNode(1);
        TokenType tokenType = getEndNodeTokenType(opNode);
        Operator op = null;
        switch (tokenType) {
            case PLUS -> op = Operator.Add;
            case MINU -> op = Operator.Sub;
        }
        return isConstExp ? calc(op, ((ConstInt) mulExpValue).getValue(), ((ConstInt) addExpValue).getValue()) : factory.createBinaryInst(curBlk, op, mulExpValue, addExpValue);
    }

    // MulExp → UnaryExp | UnaryExp ('*' | '/' | '%') MulExp
    private Value visitMulExp(VNode mulExpNode) {
        Value unaryExpValue = visitUnaryExp(mulExpNode.get1stChildNode());
        if (mulExpNode.getChildrenNodes().size() == 1) {
            return unaryExpValue;
        }
        Value mulExpValue = visitMulExp(mulExpNode.getChildNode(2));
        VNode opNode = mulExpNode.getChildNode(1);
        TokenType tokenType = getEndNodeTokenType(opNode);
        Operator op = null;
        switch (tokenType) {
            case MULT -> op = Operator.Mul;
            case DIV -> op = Operator.Div;
            case MOD -> op = Operator.Mod;
        }
        return isConstExp ? calc(op, ((ConstInt) unaryExpValue).getValue(), ((ConstInt) mulExpValue).getValue()) : factory.createBinaryInst(curBlk, op, unaryExpValue, mulExpValue);
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
        // TODO: 还没有涉及到函数调用
        return null;
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
