/**
 * 错误处理类
 * 在此手动处理除了i, j, k, a之外的错误
 */
package error;

import node.NodeType;
import node.VNode;
import symbol.*;
import token.TokenType;
import utils.IOUtils;

import java.util.*;

public class ErrorHandler {
    private static ErrorHandler instance = null;

    private ErrorHandler() {
    }

    public static ErrorHandler getInstance() {
        if (instance == null) {
            instance = new ErrorHandler();
        }
        return instance;
    }

    private final Set<Error> errorSet = new HashSet<>();

    private int inLoop = 0;

    private SymbolTable currentTable = null;
    private FuncType currentFuncType = null;

    public void addError(Error error) {
        errorSet.add(error);
    }

    public void printErrors2Buffer() {
        List<Error> toSort = new ArrayList<>(errorSet);
        toSort.sort(null);
        for (Error error : toSort) {
            IOUtils.appendBuffer(error.toString());
        }
    }

    enum ReturnType {
        VOID, INT
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
        String s = endNode.getValue();
        return s.substring(s.indexOf(" ") + 1);
    }

    // CompUnit → {Decl} {FuncDef} MainFuncDef
    public void CompUnitError(VNode compUnitNode) {
        currentTable = new SymbolTable(null);
        for (VNode child : compUnitNode.getChildrenNodes()) {
            switch (child.getNodeType()) {
                case Decl -> DeclError(child);
                case FuncDef -> FuncDefError(child);
                case MainFuncDef -> MainFuncDefError(child);
            }
        }
    }

    // Decl → ConstDecl | VarDecl
    private void DeclError(VNode declNode) {
        VNode child = declNode.get1stChildNode();
        switch (child.getNodeType()) {
            case ConstDecl -> ConstDeclError(child);
            case VarDecl -> VarDeclError(child);
        }
    }

    // ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';' // i handled in Parser
    private void ConstDeclError(VNode constDeclNode) {
        for (VNode node : constDeclNode.getChildrenNodes()) {
            if (node.getNodeType() == NodeType.ConstDef) {
                ConstDefError(node);
            }
        }
    }

    // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal  // b k, k handled in Parser
    private void ConstDefError(VNode constDefNode) {
        VNode identNode = constDefNode.get1stChildNode();
        if (currentTable.getSymbolByName(getEndNodeValue(identNode), false) != null) {
            addError(new Error(ErrorType.b, identNode.getLine()));
        }
        int dimension = 0;
        List<VNode> childrenNodes = constDefNode.getChildrenNodes();
        for (VNode node : childrenNodes) {
            switch (node.getNodeType()) {
                case ConstExp -> {
                    ConstExpError(node);
                    dimension++;
                }
                case ConstInitVal -> ConstInitValError(node);
            }
        }
        currentTable.addSymbol(new ArraySymbol(getEndNodeValue(identNode), dimension, true));
    }

    // ConstInitVal → ConstExp
    //    | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    private void ConstInitValError(VNode constInitValNode) {
        VNode child = constInitValNode.get1stChildNode();
        switch (child.getNodeType()) {
            case ConstExp -> ConstExpError(child);
            case EndNode -> {
                for (VNode node : constInitValNode.getChildrenNodes()) {
                    if (node.getNodeType() == NodeType.ConstInitVal) {
                        ConstInitValError(node);
                    }
                }
            }
        }
    }

    // VarDecl → BType VarDef { ',' VarDef } ';' // i handled in Parser
    private void VarDeclError(VNode varDeclNode) {
        for (VNode node : varDeclNode.getChildrenNodes()) {
            if (node.getNodeType() == NodeType.VarDef) {
                VarDefError(node);
            }
        }
    }

    // TODO: 在符号表中加入int/float之类的类型检测机制
    // VarDef → Ident { '[' ConstExp ']' } // b
    //    | Ident { '[' ConstExp ']' } '=' InitVal // k handled in Parser
    private void VarDefError(VNode varDefNode) {
        VNode identNode = varDefNode.get1stChildNode();
        if (currentTable.getSymbolByName(getEndNodeValue(identNode), false) != null) {
            addError(new Error(ErrorType.b, identNode.getLine()));
        }
        int dimension = 0;
        List<VNode> childrenNodes = varDefNode.getChildrenNodes();
        for (VNode node : childrenNodes) {
            switch (node.getNodeType()) {
                case ConstExp -> {
                    ConstExpError(node);
                    dimension++;
                }
                case InitVal -> InitValError(node);
            }
        }
        currentTable.addSymbol(new ArraySymbol(getEndNodeValue(identNode), dimension, false));
    }

    // InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
    private void InitValError(VNode initValNode) {
        VNode child = initValNode.get1stChildNode();
        switch (child.getNodeType()) {
            case Exp -> ExpError(child);
            case EndNode -> {
                for (VNode node : initValNode.getChildrenNodes()) {
                    if (node.getNodeType() == NodeType.InitVal) {
                        InitValError(node);
                    }
                }
            }
        }
    }

    /**
     * 从语法分析树的函数类型节点获取符号表的函数类型
     * @param funcTypeNode 语法分析的函数类型节点
     * @return 符号表的函数类型, 形如FuncType.VOID
     */
    private FuncType getFuncType(VNode funcTypeNode) {
        VNode endNode = funcTypeNode.get1stChildNode();
        String funcType = getEndNodeValue(endNode);
        if (funcType.equals("void")) {
            return FuncType.VOID;
        } else if (funcType.equals("int")) {
            return FuncType.INT;
        }
        return null;
    }

    // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block // b g j, j handled in Parser
    private void FuncDefError(VNode funcDefNode) {
        FuncType funcType = getFuncType(funcDefNode.get1stChildNode());
        currentFuncType = funcType;
        VNode identNode = funcDefNode.getChildNode(1);
        if (currentTable.getSymbolByName(getEndNodeValue(identNode), false) != null) {
            addError(new Error(ErrorType.b, identNode.getLine()));
            // 出现了函数重定义就直接跳过函数不理会里面可能出现的错误
            return;
        }
        SymbolTable nextTable = new SymbolTable(currentTable);
        currentTable.addChildTable(nextTable);
        // 切换当前的符号表
        currentTable = nextTable;
        // 第4个节点可能是FuncFParams也可能是')'
        VNode fourthNode = funcDefNode.getChildNode(3);
        // 参数列表默认为空
        List<ArraySymbol> params = new ArrayList<>();
        if (fourthNode.getNodeType() == NodeType.FuncFParams) {
            // 此时参数存在, 则应在符号表中添加参数, TODOn: 此时在FuncFParamsError中要填写参数列表
            FuncFParamsError(fourthNode, params);
        }
        currentTable.getParentTable().addSymbol(new symbol.FuncSymbol(getEndNodeValue(identNode), funcType, params));
        VNode blockNode = funcDefNode.getLastChildNode();
        // TODOn: 此时在BlockError中要返回return的类型
        ReturnType returnType = BlockError(blockNode);
        // 切换回父符号表
        currentTable = currentTable.getParentTable();
        if (funcType == FuncType.INT && returnType == ReturnType.VOID) {
            addError(new Error(ErrorType.g, blockNode.getLine()));
        }
        currentFuncType = null;
    }

    // MainFuncDef → 'int' 'main' '(' ')' Block // g j, j handled in Parser
    private void MainFuncDefError(VNode mainFuncDefNode) {
        VNode blockNode = mainFuncDefNode.getLastChildNode();
        currentFuncType = FuncType.INT;
        SymbolTable nextTable = new SymbolTable(currentTable);
        currentTable.addChildTable(nextTable);
        // 切换当前的符号表
        currentTable = nextTable;
        ReturnType returnType = BlockError(blockNode);
        // 切换回父符号表
        currentTable = currentTable.getParentTable();
        if (returnType == ReturnType.VOID) {
            addError(new Error(ErrorType.g, blockNode.getLine()));
        }
        currentFuncType = null;
    }

    // FuncFParams → FuncFParam { ',' FuncFParam }, TODOn: 此时在FuncFParamsError中要填写参数列表, 同时在此将参数分别作为临时变量加入符号表
    private void FuncFParamsError(VNode funcFParamsNode, List<ArraySymbol> params) {
        for (VNode node : funcFParamsNode.getChildrenNodes()) {
            if (node.getNodeType() == NodeType.FuncFParam) {
                ArraySymbol param = FuncFParamError(node);
                params.add(param);
                currentTable.addSymbol(param);
            }
        }
    }

    // FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]  //   b k, k handled in Parser, TODOn: 返回参数符号表对象
    private ArraySymbol FuncFParamError(VNode funcFParamNode) {
        VNode identNode = funcFParamNode.getChildNode(1);
        if (currentTable.getSymbolByName(getEndNodeValue(identNode), false) != null) {
            addError(new Error(ErrorType.b, identNode.getLine()));
        }
        int dimension = 0;
        List<VNode> childrenNodes = funcFParamNode.getChildrenNodes();
        for (VNode node : childrenNodes) {
            if (node.getNodeType() == NodeType.ConstExp) {
                ConstExpError(node);
            } else if (node.getNodeType() == NodeType.EndNode && getEndNodeTokenType(node) == TokenType.LBRACK) {
                dimension++;
            }
        }
        return new ArraySymbol(getEndNodeValue(identNode), dimension, false);
    }

    // Block → '{' { BlockItem } '}' TODOn: 此时在BlockError中要返回return的类型
    private ReturnType BlockError(VNode blockNode) {
        ReturnType returnType = ReturnType.VOID;
        for (VNode node : blockNode.getChildrenNodes()) {
            if (node.getNodeType() == NodeType.BlockItem) {
                // TODOn: 在BlockItemError中要返回return的类型, 最后一个BlockItem的返回值作为Block的返回值
                returnType = BlockItemError(node);
            }
        }
        return returnType;
    }

    // BlockItem → Decl | Stmt
    private ReturnType BlockItemError(VNode blockItemNode) {
        VNode child = blockItemNode.get1stChildNode();
        switch (child.getNodeType()) {
            case Decl -> DeclError(child);
            case Stmt -> {
                // TODOn: 在StmtError中要返回return的类型
                ReturnType returnType = StmtError(child);
                if (returnType == ReturnType.INT) {
                    return ReturnType.INT;
                }
            }
        }
        return ReturnType.VOID;
    }

    private String getLValNodeName(VNode lValNode) {
        return getEndNodeValue(lValNode.get1stChildNode());
    }

    // Stmt → LVal '=' Exp ';' | [Exp] ';' | Block // h i
    //    | 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // j
    //    | 'for' '('[ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
    //    | 'break' ';' | 'continue' ';' // i m
    //    | 'return' [Exp] ';' // f i
    //    | LVal '=' 'getint''('')'';' // h i j
    //    | 'printf''('FormatString{,Exp}')'';' // i j l
    // i, j handled in Parser
    private ReturnType StmtError(VNode stmtNode) {
        ReturnType returnType = ReturnType.VOID;
        List<VNode> childrenNodes = stmtNode.getChildrenNodes();
        VNode firstNode = stmtNode.get1stChildNode();
        boolean addedInLoop = false;
        // 第一个节点为EndNode时(只处理错误而不递归遍历字数)
        if (firstNode.getNodeType() == NodeType.EndNode) {
            switch (getEndNodeTokenType(firstNode)) {
                // 'for' '('[ForStmt] ';' [Cond] ';' [ForStmt] ')' Stmt
                case FORTK -> {
                    inLoop++;
                    addedInLoop = true;
                }
                // 'break' ';' | 'continue' ';', m
                case BREAKTK, CONTINUETK -> {
                    if (inLoop <= 0) {
                        addError(new Error(ErrorType.m, firstNode.getLine()));
                    }
                }
                // 'return' [Exp] ';', f
                case RETURNTK -> {
                    if (childrenNodes.size() > 2) {
                        returnType = ReturnType.INT;
                        if (currentFuncType == FuncType.VOID) {
                            addError(new Error(ErrorType.f, firstNode.getLine()));
                        }
                    }
                }
                // 'printf''('FormatString{,Exp}')'';', l
                case PRINTFTK -> {
                    VNode thirdNode = stmtNode.getChildNode(2);
                    if (thirdNode.getNodeType() == NodeType.EndNode) {
                        String str = getEndNodeValue(thirdNode);
                        int modelCnt = 0, expCnt = 0;
                        for (int i = 0; i < str.length(); ++i) {
                            if (str.charAt(i) == '%' && i + 1 < str.length() && str.charAt(i + 1) == 'd') {
                                modelCnt++;
                            }
                        }
                        for (VNode node : childrenNodes) {
                            if (node.getNodeType() == NodeType.Exp) {
                                expCnt++;
                            }
                        }
                        if (modelCnt != expCnt) {
                            addError(new Error(ErrorType.l, firstNode.getLine()));
                        }
                    }
                }
            }
        }
        for (VNode node : childrenNodes) {
            switch (node.getNodeType()) {
                // TODOn: h - 左值不能是常量(不判断是否为函数, 如果匹配的为函数名, 应报错"符号未定义")
                case LVal -> {
                    String name = getLValNodeName(node);
                    Symbol symbol = currentTable.getSymbolByName(name, true);
                    if (symbol instanceof ArraySymbol && ((ArraySymbol) symbol).isConst()) {
                        addError(new Error(ErrorType.h, node.getLine()));
                    }
                    LValError(node);
                }
                case Exp -> ExpError(node);
                case Block -> {
                    SymbolTable nextTable = new SymbolTable(currentTable);
                    currentTable.addChildTable(nextTable);
                    // 切换当前的符号表
                    currentTable = nextTable;
                    returnType = BlockError(node);
                    // 切换回父符号表
                    currentTable = currentTable.getParentTable();
                }
                case Cond -> CondError(node);
                case Stmt -> returnType = StmtError(node);
                case ForStmt -> ForStmtError(node);
            }
        }
        if (addedInLoop) {
            inLoop--;
        }
        return returnType;
    }

    // ForStmt → LVal '=' Exp   //h
    private void ForStmtError(VNode forStmtNode) {
        VNode lValNode = forStmtNode.get1stChildNode();
        String name = getLValNodeName(lValNode);
        Symbol symbol = currentTable.getSymbolByName(name, true);
        if (symbol instanceof ArraySymbol && ((ArraySymbol) symbol).isConst()) {
            addError(new Error(ErrorType.h, lValNode.getLine()));
        }
        LValError(lValNode);
        ExpError(forStmtNode.getChildNode(2));
    }

    // Exp → AddExp 注：SysY 表达式是int 型表达式
    private ArraySymbol ExpError(VNode expNode) {
        return AddExpError(expNode.get1stChildNode());
    }

    // Cond → LOrExp
    private void CondError(VNode condNode) {
        LOrExpError(condNode.get1stChildNode());
    }

    // LVal → Ident {'[' Exp ']'} // c k, k handled in Parser
    private ArraySymbol LValError(VNode lValNode) {
        VNode identNode = lValNode.get1stChildNode();
        Symbol symbol = currentTable.getSymbolByName(getEndNodeValue(identNode), true);
        if (!(symbol instanceof ArraySymbol)) {
            addError(new Error(ErrorType.c, identNode.getLine()));
        }
        int dimension = 0;
        if (symbol instanceof ArraySymbol arraySymbol) {
            dimension = arraySymbol.getDimension();
        }
        List<VNode> childrenNodes = lValNode.getChildrenNodes();
        for (VNode node : childrenNodes) {
            if (node.getNodeType() == NodeType.Exp) {
                --dimension;
                ExpError(node);
            }
        }
        return new ArraySymbol(getEndNodeValue(identNode), Math.max(0, dimension), false);
    }

    // PrimaryExp → '(' Exp ')' | LVal | Number
    private ArraySymbol PrimaryExpError(VNode primaryExpNode) {
        for (VNode node : primaryExpNode.getChildrenNodes()) {
            switch (node.getNodeType()) {
                case Exp -> ExpError(node);
                case LVal -> {
                    return LValError(node);
                }
            }
        }
        return new ArraySymbol("", 0, false);
    }

    // UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' // c d e j, j handled in Parser
    //        | UnaryOp UnaryExp
    private ArraySymbol UnaryExpError(VNode unaryExpNode) {
        VNode firstNode = unaryExpNode.get1stChildNode();
        switch (firstNode.getNodeType()) {
            case PrimaryExp -> {
                return PrimaryExpError(firstNode);
            }
            case UnaryOp -> UnaryExpError(unaryExpNode.getChildNode(1));
            case EndNode -> {
                TokenType tokenType = getEndNodeTokenType(firstNode);
                if (tokenType == TokenType.IDENFR) {
                    String name = getEndNodeValue(firstNode);
                    Symbol symbol = currentTable.getSymbolByName(name, true);
                    if (!(symbol instanceof FuncSymbol funcSymbol)) {
                        addError(new Error(ErrorType.c, firstNode.getLine()));
                        return new ArraySymbol("", 0, false);
                    }
                    List<ArraySymbol> fParams = funcSymbol.getParams();
                    List<ArraySymbol> rParams = new ArrayList<>();
                    VNode thirdNode = unaryExpNode.getChildNode(2);
                    if (thirdNode != null && thirdNode.getNodeType() == NodeType.FuncRParams) {
                        // TODOn: FuncRParamsError要返回参数列表
                        rParams = FuncRParamsError(thirdNode);
                    }
                    if (fParams.size() != rParams.size()) {
                        addError(new Error(ErrorType.d, firstNode.getLine()));
                    } else {
                        for (int i = 0; i < fParams.size(); ++i) {
                            ArraySymbol fParam = fParams.get(i);
                            ArraySymbol rParam = rParams.get(i);
                            if (fParam.getDimension() != rParam.getDimension()) {
                                addError(new Error(ErrorType.e, firstNode.getLine()));
                            }
                        }
                    }
                    return new ArraySymbol(funcSymbol.getName(), funcSymbol.getType() == FuncType.INT ? 0 : -1, false);
                }
            }
        }
        return new ArraySymbol("", 0, false);
    }

    // FuncRParams → Exp { ',' Exp }
    private List<ArraySymbol> FuncRParamsError(VNode funcRParamsNode) {
        List<ArraySymbol> rParams = new ArrayList<>();
        for (VNode node : funcRParamsNode.getChildrenNodes()) {
            if (node.getNodeType() == NodeType.Exp) {
                rParams.add(ExpError(node));
            }
        }
        return rParams;
    }

    // MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    private ArraySymbol MulExpError(VNode mulExpNode) {
        List<VNode> childrenNodes = mulExpNode.getChildrenNodes();
        if (childrenNodes.size() == 1) {
            return UnaryExpError(childrenNodes.get(0));
        }
        for (VNode node : childrenNodes) {
            switch (node.getNodeType()) {
                case UnaryExp -> UnaryExpError(node);
                case MulExp -> MulExpError(node);
            }
        }
        return new ArraySymbol("", 0, false);
    }

    // AddExp → MulExp | AddExp ('+' | '−') MulExp
    private ArraySymbol AddExpError(VNode addExpNode) {
        List<VNode> childrenNodes = addExpNode.getChildrenNodes();
        if (childrenNodes.size() == 1) {
            return MulExpError(childrenNodes.get(0));
        }
        for (VNode node : childrenNodes) {
            switch (node.getNodeType()) {
                case MulExp -> MulExpError(node);
                case AddExp -> AddExpError(node);
            }
        }
        return new ArraySymbol("", 0, false);
    }

    // RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    private ArraySymbol RelExpError(VNode relExpNode) {
        List<VNode> childrenNodes = relExpNode.getChildrenNodes();
        if (childrenNodes.size() == 1) {
            return AddExpError(childrenNodes.get(0));
        }
        for (VNode node : childrenNodes) {
            switch (node.getNodeType()) {
                case AddExp -> AddExpError(node);
                case RelExp -> RelExpError(node);
            }
        }
        return new ArraySymbol("", 0, false);
    }

    // EqExp → RelExp | EqExp ('==' | '!=') RelExp
    private ArraySymbol EqExpError(VNode eqExpNode) {
        List<VNode> childrenNodes = eqExpNode.getChildrenNodes();
        if (childrenNodes.size() == 1) {
            return RelExpError(childrenNodes.get(0));
        }
        for (VNode node : childrenNodes) {
            switch (node.getNodeType()) {
                case RelExp -> RelExpError(node);
                case EqExp -> EqExpError(node);
            }
        }
        return new ArraySymbol("", 0, false);
    }

    // LAndExp → EqExp | LAndExp '&&' EqExp
    private ArraySymbol LAndExpError(VNode lAndExpNode) {
        List<VNode> childrenNodes = lAndExpNode.getChildrenNodes();
        if (childrenNodes.size() == 1) {
            return EqExpError(childrenNodes.get(0));
        }
        for (VNode node : childrenNodes) {
            switch (node.getNodeType()) {
                case EqExp -> EqExpError(node);
                case LAndExp -> LAndExpError(node);
            }
        }
        return new ArraySymbol("", 0, false);
    }

    // LOrExp → LAndExp | LOrExp '||' LAndExp
    private ArraySymbol LOrExpError(VNode lOrExpNode) {
        List<VNode> childrenNodes = lOrExpNode.getChildrenNodes();
        if (childrenNodes.size() == 1) {
            return LAndExpError(childrenNodes.get(0));
        }
        for (VNode node : childrenNodes) {
            switch (node.getNodeType()) {
                case LAndExp -> LAndExpError(node);
                case LOrExp -> LOrExpError(node);
            }
        }
        return new ArraySymbol("", 0, false);
    }

    // ConstExp → AddExp 注：使用的Ident 必须是常量
    private ArraySymbol ConstExpError(VNode constExpNode) {
        return AddExpError(constExpNode.get1stChildNode());
    }
}
