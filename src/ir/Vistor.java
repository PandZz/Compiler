package ir;

import ir.type.IntType;
import ir.value.BasicBlock;
import ir.value.ConstInt;
import ir.value.Function;
import ir.value.Value;
import ir.value.instructions.Operator;
import ir.value.instructions.terminator.RetInst;
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
    private BasicBlock curBlk;
    private Function curFunc;
    /**
     * 当前的临时变量编号, 注意这个编号在每个函数中都是从0开始的, 且编号还会分配给基本块
     */
    private Vistor() {
        factory = IRBuildFactory.getInstance();
        symTlbs = new ArrayList<>();
    }

    public static Vistor getInstance() {
        if (instance == null)
            instance = new Vistor();
        return instance;
    }

    private void switchBlk() {
        curBlk = factory.createBasicBlock(curFunc);
    }

    private Map<String, Value> getCurSymTbl() {
        return symTlbs.get(symTlbs.size() - 1);
    }

    /**
     * 递归地在符号表中寻找匹配的符号
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

    private void addSymbol(String name, Value value) {
        getCurSymTbl().put(name, value);
    }

    private void pushSymTbl() {
        symTlbs.add(new HashMap<>());
    }

    private void popSymTbl() {
        symTlbs.remove(symTlbs.size() - 1);
    }

    private final Map<String, TokenType> tokenTypeMap = new HashMap<>() {{
        for (TokenType tokenType : TokenType.values()) {
            put(tokenType.toString(), tokenType);
        }
    }};
    /**
     * 从语法树中的终结符节点获取其TokenType
     * @param endNode 终结符
     * @return TokenType枚举
     */
    private TokenType getEndNodeTokenType(VNode endNode) {
        String s = endNode.getValue().split(" ")[0];
        return tokenTypeMap.getOrDefault(s, null);
    }

    /**
     * 从语法树中的终结符节点获取其值
     * @param endNode 终结符
     * @return 形如";"或"a"的字符串
     */
    private String getEndNodeValue(VNode endNode) {
        return endNode.getValue().split(" ")[1];
    }

    // CompUnit → {Decl} {FuncDef} MainFuncDef
    public void visitCompUnit(VNode CompUnitNode) {
        pushSymTbl();
        for (VNode node : CompUnitNode.getChildrenNodes()) {
            switch (node.getNodeType()) {
//                case Decl -> visitDecl(node);
//                case FuncDef -> visitFuncDef(node);
                case MainFuncDef -> visitMainFuncDef(node);
            }
        }
        popSymTbl();
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
        pushSymTbl();
        for (VNode node : blockNode.getChildrenNodes()) {
            switch (node.getNodeType()) {
                case BlockItem -> visitBlockItem(node);
            }
        }
        popSymTbl();
    }

    // BlockItem → Decl | Stmt
    private void visitBlockItem(VNode blockItemNode) {
        switch (blockItemNode.get1stChildNode().getNodeType()) {
//            case Decl -> visitDecl(blockItemNode.get1stChildNode());
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
//            case LVal -> visitLVal(firstChildNode);
//            case Exp -> visitExp(firstChildNode);
//            case Block -> visitBlock(firstChildNode);
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
        return visitAddExp(expNode.get1stChildNode());
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
        return factory.createBinaryInst(curBlk, op, mulExpValue, addExpValue);
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
        return factory.createBinaryInst(curBlk, op, unaryExpValue, mulExpValue);
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
            switch (tokenType) {
                case PLUS -> op = Operator.Add;
                case MINU -> op = Operator.Sub;
                case NOT -> {
                    return factory.createNotInst(curBlk, visitUnaryExp(unaryExpNode.getChildNode(1)));
                }
            }
            return factory.createBinaryInst(curBlk, op, ConstInt.ZERO, visitUnaryExp(unaryExpNode.getChildNode(1)));
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
                    String lValName = getEndNodeValue(firstChildNode);
                    return findSym(lValName);
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
}
