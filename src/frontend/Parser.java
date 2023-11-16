/**
 * Parser 递归子程序分析token序列生成AST抽象语法树
 * 在这里处理语法错误: i, j, k 以及 a
 */
package frontend;

import config.Config;
import error.*;
import error.Error;
import node.*;
import token.*;

import java.util.*;

public class Parser {
    private static Parser instance = null;
    private List<Token> tokens;
    private int nowTokenIndex = 0;
    private VNode compUnitNode;
    private final ErrorHandler errorHandler = ErrorHandler.getInstance();
    private final Map<NodeType, Set<TokenType>> FIRST = new HashMap<>() {{
        put(NodeType.FuncRParams, new HashSet<>() {{
            add(TokenType.LPARENT);
            add(TokenType.IDENFR);
            add(TokenType.INTCON);
            add(TokenType.PLUS);
            add(TokenType.MINU);
            add(TokenType.NOT);
        }});
    }};

    private Parser() {
    }

    public static Parser getInstance() {
        if (instance == null) {
            instance = new Parser();
        }
        return instance;
    }

    public VNode transTokens2VNode(List<Token> tokens) {
        this.tokens = tokens;
        this.nowTokenIndex = 0;
        this.compUnitNode = null;
        return getCompUnitNode();
    }

    public VNode getCompUnitNode() {
        if (compUnitNode == null) {
            parse();
        }
        return compUnitNode;
    }

    private void parse() {
        if (tokens == null) {
            throw new RuntimeException("token list is empty");
        }
        this.compUnitNode = CompUnit();
    }

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Token now() {
        return tokens.get(nowTokenIndex);
    }

    private Token last() {
        return tokens.get(nowTokenIndex - 1);
    }

    private Token next(int i) {
        return tokens.get(nowTokenIndex + i);
    }

    private Token expect(TokenType type) {
        if (now().getType() != type) {
            throw new RuntimeException("Expect " + type + " but got " + now().getType() + "\n" + "token info: " + now().toDebugString());
        }
        Token token = now();
        if (this.nowTokenIndex + 1 < tokens.size()) {
            ++this.nowTokenIndex;
        }
        return token;
    }

    private VNode CompUnit() {
        // CompUnit -> {Decl} {FuncDef} MainFuncDef
        List<VNode> childrenNodes = new ArrayList<>();
        while (next(1).getType() != TokenType.MAINTK && next(2).getType() != TokenType.LPARENT) {
            VNode declNode = Decl();
            childrenNodes.add(declNode);
        }
        while (next(1).getType() != TokenType.MAINTK) {
            VNode funcDefNode = FuncDef();
            childrenNodes.add(funcDefNode);
        }
        VNode mainFuncDefNode = MainFuncDef();
        childrenNodes.add(mainFuncDefNode);
        return new VNode(childrenNodes, NodeType.CompUnit);
    }

    private VNode Decl() {
        // Decl -> ConstDecl | VarDecl
        List<VNode> childrenNodes = new ArrayList<>();
        if (now().getType() == TokenType.CONSTTK) {
            childrenNodes.add(ConstDecl());
        } else {
            childrenNodes.add(VarDecl());
        }
        return new VNode(childrenNodes, NodeType.Decl);
    }

    private VNode ConstDecl() {
        // ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(new VNode(expect(TokenType.CONSTTK)));
        childrenNodes.add(BType());
        childrenNodes.add(ConstDef());
        while (now().getType() == TokenType.COMMA) {
            childrenNodes.add(new VNode(expect(TokenType.COMMA)));
            childrenNodes.add(ConstDef());
        }
        handleSEMICNError(childrenNodes);
        return new VNode(childrenNodes, NodeType.ConstDecl);
    }

    private void handleSEMICNError(List<VNode> childrenNodes) {
        try {
            childrenNodes.add(new VNode(expect(TokenType.SEMICN)));
        } catch (RuntimeException e) {
            if (Config.ERROR) {
                errorHandler.addError(new Error(ErrorType.i, last().getLine()));
            }
            if (Config.DEBUG) {
                System.out.printf("in line %d: '%s' should be followed by ';'%n", last().getLine(), last().getValue());
            }
        }
    }

    private VNode BType() {
        // BType -> 'int'
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(new VNode(expect(TokenType.INTTK)));
        return new VNode(childrenNodes, NodeType.BType);
    }

    private VNode ConstDef() {
        // ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
        List<VNode> childrenNodes = new ArrayList<>();
        Token idenfrToken = expect(TokenType.IDENFR);
        childrenNodes.add(new VNode(idenfrToken));
        while (now().getType() == TokenType.LBRACK) {
            childrenNodes.add(new VNode(expect(TokenType.LBRACK)));
            childrenNodes.add(ConstExp());
            handleRBRACKError(childrenNodes);
        }
        childrenNodes.add(new VNode(expect(TokenType.ASSIGN)));
        childrenNodes.add(ConstInitVal());
        return new VNode(childrenNodes, NodeType.ConstDef);
    }

    private VNode ConstInitVal() {
        // ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
        List<VNode> childrenNodes = new ArrayList<>();
        if (now().getType() == TokenType.LBRACE) {
            childrenNodes.add(new VNode(expect(TokenType.LBRACE)));
            if (now().getType() != TokenType.RBRACE) {
                childrenNodes.add(ConstInitVal());
                while (now().getType() == TokenType.COMMA) {
                    childrenNodes.add(new VNode(expect(TokenType.COMMA)));
                    childrenNodes.add(ConstInitVal());
                }
            }
            childrenNodes.add(new VNode(expect(TokenType.RBRACE)));
        } else {
            childrenNodes.add(ConstExp());
        }
        return new VNode(childrenNodes, NodeType.ConstInitVal);
    }

    private VNode VarDecl() {
        // VarDecl -> BType VarDef { ',' VarDef } ';'
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(BType());
        childrenNodes.add(VarDef());
        while (now().getType() == TokenType.COMMA) {
            childrenNodes.add(new VNode(expect(TokenType.COMMA)));
            childrenNodes.add(VarDef());
        }
        handleSEMICNError(childrenNodes);
        return new VNode(childrenNodes, NodeType.VarDecl);
    }

    private VNode VarDef() {
        // VarDef → Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
        List<VNode> childrenNodes = new ArrayList<>();
        Token idenfrToken = expect(TokenType.IDENFR);
        childrenNodes.add(new VNode(idenfrToken));
        while (now().getType() == TokenType.LBRACK) {
            childrenNodes.add(new VNode(expect(TokenType.LBRACK)));
            childrenNodes.add(ConstExp());
            handleRBRACKError(childrenNodes);
        }
        if (now().getType() == TokenType.ASSIGN) {
            childrenNodes.add(new VNode(expect(TokenType.ASSIGN)));
            childrenNodes.add(InitVal());
        }
        return new VNode(childrenNodes, NodeType.VarDef);
    }

    private VNode InitVal() {
        // InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
        List<VNode> childrenNodes = new ArrayList<>();
        if (now().getType() == TokenType.LBRACE) {
            childrenNodes.add(new VNode(expect(TokenType.LBRACE)));
            if (now().getType() != TokenType.RBRACE) {
                childrenNodes.add(InitVal());
                while (now().getType() == TokenType.COMMA) {
                    childrenNodes.add(new VNode(expect(TokenType.COMMA)));
                    childrenNodes.add(InitVal());
                }
            }
            childrenNodes.add(new VNode(expect(TokenType.RBRACE)));
        } else {
            childrenNodes.add(Exp());
        }
        return new VNode(childrenNodes, NodeType.InitVal);
    }

    private VNode FuncDef() {
        // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
        // Fi(FuncFParams) = Fi(BType) = { 'int' }
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(FuncType());
        Token idenfrToken = expect(TokenType.IDENFR);
        childrenNodes.add(new VNode(idenfrToken));
        childrenNodes.add(new VNode(expect(TokenType.LPARENT)));
        if (now().getType() == TokenType.INTTK) {
            childrenNodes.add(FuncFParams());
        }
        handlePRARENTError(childrenNodes);
        childrenNodes.add(Block());
        return new VNode(childrenNodes, NodeType.FuncDef);
    }

    private void handlePRARENTError(List<VNode> childrenNodes) {
        try {
            childrenNodes.add(new VNode(expect(TokenType.RPARENT)));
        } catch (RuntimeException e) {
            if (Config.ERROR) {
                errorHandler.addError(new Error(ErrorType.j, last().getLine()));
            }
            if (Config.DEBUG) {
                System.out.printf("in line %d: missing ')' after '%s'%n", last().getLine(), last().getValue());
            }
        }
    }

    private VNode MainFuncDef() {
        // MainFuncDef → 'int' 'main' '(' ')' Block
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(new VNode(expect(TokenType.INTTK)));
        childrenNodes.add(new VNode(expect(TokenType.MAINTK)));
        childrenNodes.add(new VNode(expect(TokenType.LPARENT)));
        handlePRARENTError(childrenNodes);
        childrenNodes.add(Block());
        return new VNode(childrenNodes, NodeType.MainFuncDef);
    }

    private VNode FuncType() {
        // FuncType → 'void' | 'int'
        List<VNode> childrenNodes = new ArrayList<>();
        if (now().getType() == TokenType.VOIDTK) {
            childrenNodes.add(new VNode(expect(TokenType.VOIDTK)));
        } else if (now().getType() == TokenType.INTTK) {
            childrenNodes.add(new VNode(expect(TokenType.INTTK)));
        }
        return new VNode(childrenNodes, NodeType.FuncType);
    }

    private VNode FuncFParams() {
        // FuncFParams → FuncFParam { ',' FuncFParam }
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(FuncFParam());
        while (now().getType() == TokenType.COMMA) {
            childrenNodes.add(new VNode(expect(TokenType.COMMA)));
            childrenNodes.add(FuncFParam());
        }
        return new VNode(childrenNodes, NodeType.FuncFParams);
    }

    private VNode FuncFParam() {
        // FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(BType());
        Token idenfrToken = expect(TokenType.IDENFR);
        childrenNodes.add(new VNode(idenfrToken));
        if (now().getType() == TokenType.LBRACK) {
            childrenNodes.add(new VNode(expect(TokenType.LBRACK)));
            handleRBRACKError(childrenNodes);
            while (now().getType() == TokenType.LBRACK) {
                childrenNodes.add(new VNode(expect(TokenType.LBRACK)));
                childrenNodes.add(ConstExp());
                handleRBRACKError(childrenNodes);
            }
        }
        return new VNode(childrenNodes, NodeType.FuncFParam);
    }

    private void handleRBRACKError(List<VNode> childrenNodes) {
        try {
            childrenNodes.add(new VNode(expect(TokenType.RBRACK)));
        } catch (RuntimeException e) {
            if (Config.ERROR) {
                errorHandler.addError(new Error(ErrorType.k, last().getLine()));
            }
            if (Config.DEBUG) {
                System.out.printf("in line %d: missing ']' after '%s'%n", last().getLine(), last().getValue());
            }
        }
    }

    private VNode Block() {
        // Block → '{' { BlockItem } '}'
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(new VNode(expect(TokenType.LBRACE)));
        while (now().getType() != TokenType.RBRACE) {
            childrenNodes.add(BlockItem());
        }
        childrenNodes.add(new VNode(expect(TokenType.RBRACE)));
        return new VNode(childrenNodes, NodeType.Block);
    }

    private VNode BlockItem() {
        // BlockItem → Decl | Stmt
        List<VNode> childrenNodes = new ArrayList<>();
        if (now().getType() == TokenType.CONSTTK || now().getType() == TokenType.INTTK) {
            childrenNodes.add(Decl());
        } else {
            childrenNodes.add(Stmt());
        }
        return new VNode(childrenNodes, NodeType.BlockItem);
    }

    private boolean hasAssignInLine() {
        int i = this.nowTokenIndex;
        while (i < tokens.size() && tokens.get(i).getLine() == now().getLine()) {
            if (tokens.get(i).getType() == TokenType.ASSIGN) {
                return true;
            }
            ++i;
        }
        return false;
    }

    private VNode Stmt() {
        /*
         * Stmt -> LVal '=' Exp ';'
         * | [Exp] ';'
         * | Block
         * | 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
         * | 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
         * | 'break' ';' | 'continue' ';'
         * | 'return' [Exp] ';'
         * | LVal '=' 'getint' '(' ')' ';'
         * | 'printf' '(' FormatString { ',' Exp } ')' ';'
         *
         * Fi(Exp) = {'+', '-', '!', '(', IDENFR, INTCON}
         * Fi(LVal) = {IDENFR}
         */
        List<VNode> childrenNodes = new ArrayList<>();
        if (now().getType() == TokenType.LBRACE) {
            // Block
            childrenNodes.add(Block());
        } else if (now().getType() == TokenType.IFTK) {
            // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
            childrenNodes.add(new VNode(expect(TokenType.IFTK)));
            childrenNodes.add(new VNode(expect(TokenType.LPARENT)));
            childrenNodes.add(Cond());
            handlePRARENTError(childrenNodes);
            childrenNodes.add(Stmt());
            if (now().getType() == TokenType.ELSETK) {
                childrenNodes.add(new VNode(expect(TokenType.ELSETK)));
                childrenNodes.add(Stmt());
            }
        } else if (now().getType() == TokenType.FORTK) {
            // 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
            // Fi(ForStmt) = Fi(LVal) = { IDENFR }
            childrenNodes.add(new VNode(expect(TokenType.FORTK)));
            childrenNodes.add(new VNode(expect(TokenType.LPARENT)));
            if (now().getType() != TokenType.SEMICN) {
                childrenNodes.add(ForStmt());
            }
            handleSEMICNError(childrenNodes);
            if (now().getType() != TokenType.SEMICN) {
                childrenNodes.add(Cond());
            }
            handleSEMICNError(childrenNodes);
            if (now().getType() == TokenType.IDENFR) {
                childrenNodes.add(ForStmt());
            }
            handlePRARENTError(childrenNodes);
            childrenNodes.add(Stmt());
        } else if (now().getType() == TokenType.BREAKTK || now().getType() == TokenType.CONTINUETK) {
            // 'break' ';' | 'continue' ';'
            if (now().getType() == TokenType.BREAKTK) {
                childrenNodes.add(new VNode(expect(TokenType.BREAKTK)));
            } else {
                childrenNodes.add(new VNode(expect(TokenType.CONTINUETK)));
            }
            handleSEMICNError(childrenNodes);
        } else if (now().getType() == TokenType.RETURNTK) {
            // 'return' [Exp] ';'
            childrenNodes.add(new VNode(expect(TokenType.RETURNTK)));
            if (now().getType() != TokenType.SEMICN) {
                childrenNodes.add(Exp());
            }
            handleSEMICNError(childrenNodes);
        } else if (now().getType() == TokenType.PRINTFTK) {
            // 'printf' '(' FormatString { ',' Exp } ')' ';'
            childrenNodes.add(new VNode(expect(TokenType.PRINTFTK)));
            childrenNodes.add(new VNode(expect(TokenType.LPARENT)));
            handleFormatStringError(childrenNodes);
            while (now().getType() == TokenType.COMMA) {
                childrenNodes.add(new VNode(expect(TokenType.COMMA)));
                childrenNodes.add(Exp());
            }
            handlePRARENTError(childrenNodes);
            handleSEMICNError(childrenNodes);
        } else {
            if (hasAssignInLine()) {
                // LVal '=' Exp ';' | LVal '=' 'getint' '(' ')' ';'
                childrenNodes.add(LVal());
                childrenNodes.add(new VNode(expect(TokenType.ASSIGN)));
                if (now().getType() == TokenType.GETINTTK) {
                    // LVal '=' 'getint' '(' ')' ';'
                    childrenNodes.add(new VNode(expect(TokenType.GETINTTK)));
                    childrenNodes.add(new VNode(expect(TokenType.LPARENT)));
                    handlePRARENTError(childrenNodes);
                    handleSEMICNError(childrenNodes);
                } else {
                    // LVal '=' Exp ';'
                    childrenNodes.add(Exp());
                    handleSEMICNError(childrenNodes);
                }
            } else {
                // [Exp] ';'
                if (now().getType() != TokenType.SEMICN) {
                    childrenNodes.add(Exp());
                }
                handleSEMICNError(childrenNodes);
            }
        }
        return new VNode(childrenNodes, NodeType.Stmt);
    }

    private VNode ForStmt() {
        // ForStmt → LVal '=' Exp
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(LVal());
        childrenNodes.add(new VNode(expect(TokenType.ASSIGN)));
        childrenNodes.add(Exp());
        return new VNode(childrenNodes, NodeType.ForStmt);
    }

    private VNode Exp() {
        // Exp → AddExp
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(AddExp());
        return new VNode(childrenNodes, NodeType.Exp);
    }

    private VNode Cond() {
        // Cond → LOrExp
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(LOrExp());
        return new VNode(childrenNodes, NodeType.Cond);
    }

    private VNode LVal() {
        // LVal → Ident {'[' Exp ']'}
        List<VNode> childrenNodes = new ArrayList<>();
        Token idenfrToken = expect(TokenType.IDENFR);
        childrenNodes.add(new VNode(idenfrToken));
        while (now().getType() == TokenType.LBRACK) {
            childrenNodes.add(new VNode(expect(TokenType.LBRACK)));
            childrenNodes.add(Exp());
            handleRBRACKError(childrenNodes);
        }
        return new VNode(childrenNodes, NodeType.LVal);
    }

    private VNode PrimaryExp() {
        // PrimaryExp → '(' Exp ')' | LVal | Number
        List<VNode> childrenNodes = new ArrayList<>();
        if (now().getType() == TokenType.LPARENT) {
            childrenNodes.add(new VNode(expect(TokenType.LPARENT)));
            childrenNodes.add(Exp());
            handlePRARENTError(childrenNodes);
        } else if (now().getType() == TokenType.IDENFR) {
            childrenNodes.add(LVal());
        } else {
            childrenNodes.add(Number());
        }
        return new VNode(childrenNodes, NodeType.PrimaryExp);
    }

    private VNode Number() {
        // Number → IntConst
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(IntConst());
        return new VNode(childrenNodes, NodeType.Number);
    }

    private boolean isUnaryOp(TokenType type) {
        return type == TokenType.PLUS || type == TokenType.MINU || type == TokenType.NOT;
    }

    private VNode UnaryExp() {
        // UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
        List<VNode> childrenNodes = new ArrayList<>();
        if (isUnaryOp(now().getType())) {
            // UnaryOp UnaryExp
            childrenNodes.add(UnaryOp());
            childrenNodes.add(UnaryExp());
        } else if (now().getType() == TokenType.IDENFR && next(1).getType() == TokenType.LPARENT) {
            // Ident '(' [FuncRParams] ')'
            // Fi(FuncRParams) = Fi(Exp) = Fi(AddExp) = Fi(MulExp) = Fi(UnaryExp)
            // = Fi(PrimaryExp) | Fi(UnaryOp) | Fi(Ident)
            // = { '(', IDENFR, INTCON, '+', '-', '!' }
            Token idenfrToken = expect(TokenType.IDENFR);
            childrenNodes.add(new VNode(idenfrToken));
            childrenNodes.add(new VNode(expect(TokenType.LPARENT)));
            if (FIRST.get(NodeType.FuncRParams).contains(now().getType())) {
                childrenNodes.add(FuncRParams());
            }
            handlePRARENTError(childrenNodes);
        } else {
            // PrimaryExp
            childrenNodes.add(PrimaryExp());
        }
        return new VNode(childrenNodes, NodeType.UnaryExp);
    }

    private VNode UnaryOp() {
        // UnaryOp → '+' | '−' | '!'
        List<VNode> childrenNodes = new ArrayList<>();
        if (now().getType() == TokenType.PLUS) {
            childrenNodes.add(new VNode(expect(TokenType.PLUS)));
        } else if (now().getType() == TokenType.MINU) {
            childrenNodes.add(new VNode(expect(TokenType.MINU)));
        } else {
            childrenNodes.add(new VNode(expect(TokenType.NOT)));
        }
        return new VNode(childrenNodes, NodeType.UnaryOp);
    }

    private VNode FuncRParams() {
        // FuncRParams → Exp { ',' Exp }
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(Exp());
        while (now().getType() == TokenType.COMMA) {
            childrenNodes.add(new VNode(expect(TokenType.COMMA)));
            childrenNodes.add(Exp());
        }
        return new VNode(childrenNodes, NodeType.FuncRParams);
    }

    private VNode MulExp() {
        // MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(UnaryExp());
        if (now().getType() == TokenType.MULT || now().getType() == TokenType.DIV || now().getType() == TokenType.MOD) {
            childrenNodes.add(new VNode(expect(now().getType())));
            childrenNodes.add(MulExp());
        }
        return new VNode(childrenNodes, NodeType.MulExp);
    }

    private VNode AddExp() {
        // AddExp → MulExp | AddExp ('+' | '−') MulExp
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(MulExp());
        if (now().getType() == TokenType.PLUS || now().getType() == TokenType.MINU) {
            childrenNodes.add(new VNode(expect(now().getType())));
            childrenNodes.add(AddExp());
        }
        return new VNode(childrenNodes, NodeType.AddExp);
    }

    private VNode RelExp() {
        // RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(AddExp());
        if (now().getType() == TokenType.LSS || now().getType() == TokenType.GRE || now().getType() == TokenType.LEQ || now().getType() == TokenType.GEQ) {
            childrenNodes.add(new VNode(expect(now().getType())));
            childrenNodes.add(RelExp());
        }
        return new VNode(childrenNodes, NodeType.RelExp);
    }

    private VNode EqExp() {
        // EqExp → RelExp | EqExp ('==' | '!=') RelExp
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(RelExp());
        if (now().getType() == TokenType.EQL || now().getType() == TokenType.NEQ) {
            childrenNodes.add(new VNode(expect(now().getType())));
            childrenNodes.add(EqExp());
        }
        return new VNode(childrenNodes, NodeType.EqExp);
    }

    private VNode LAndExp() {
        // LAndExp → EqExp | LAndExp '&&' EqExp
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(EqExp());
        if (now().getType() == TokenType.AND) {
            childrenNodes.add(new VNode(expect(TokenType.AND)));
            childrenNodes.add(LAndExp());
        }
        return new VNode(childrenNodes, NodeType.LAndExp);
    }

    private VNode LOrExp() {
        // LOrExp → LAndExp | LOrExp '||' LAndExp
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(LAndExp());
        if (now().getType() == TokenType.OR) {
            childrenNodes.add(new VNode(expect(TokenType.OR)));
            childrenNodes.add(LOrExp());
        }
        return new VNode(childrenNodes, NodeType.LOrExp);
    }

    private VNode ConstExp() {
        // ConstExp → AddExp
        List<VNode> childrenNodes = new ArrayList<>();
        childrenNodes.add(AddExp());
        return new VNode(childrenNodes, NodeType.ConstExp);
    }

    private VNode IntConst() {
        return new VNode(expect(TokenType.INTCON));
    }

    private boolean isCharInRange(char ch) {
        return ch == 32 || ch == 33 || (ch >= 40 && ch <= 126);
    }

    private void handleFormatStringError(List<VNode> childrenNodes) {
        Token strToken = expect(TokenType.STRCON);
        childrenNodes.add(new VNode(strToken));
        String str = strToken.getValue();
        if (str.startsWith("\"") && str.endsWith("\"")) {
            str = str.substring(1, str.length() - 1);
        } else {
            if (Config.ERROR) {
                errorHandler.addError(new Error(ErrorType.a, strToken.getLine()));
            }
            if (Config.DEBUG) {
                System.out.printf("in line %d: '%s' is not a valid string constant%n", strToken.getLine(), str);
            }
            return;
        }
        for (int i = 0; i < str.length(); ++i) {
            char ch = str.charAt(i);
            if (ch == '\\') {
                if (i + 1 >= str.length() || str.charAt(i + 1) != 'n') {
                    if (Config.ERROR) {
                        errorHandler.addError(new Error(ErrorType.a, strToken.getLine()));
                    }
                    if (Config.DEBUG) {
                        System.out.printf("in line %d: '%s' is not a valid string constant%n", strToken.getLine(), str);
                    }
                    break;
                }
            } else if (ch == '%') {
                if (i + 1 >= str.length() || str.charAt(i + 1) != 'd') {
                    if (Config.ERROR) {
                        errorHandler.addError(new Error(ErrorType.a, strToken.getLine()));
                    }
                    if (Config.DEBUG) {
                        System.out.printf("in line %d: '%s' is not a valid string constant%n", strToken.getLine(), str);
                    }
                    break;
                }
            } else if (!isCharInRange(ch)) {
                if (Config.ERROR) {
                    errorHandler.addError(new Error(ErrorType.a, strToken.getLine()));
                }
                if (Config.DEBUG) {
                    System.out.printf("in line %d: '%s' is not a valid string constant%n", strToken.getLine(), str);
                }
                break;
            }
        }
    }
}
