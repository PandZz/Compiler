package node;

public enum NodeType {
    CompUnit,
    Decl,
    FuncDef,
    MainFuncDef,
    ConstDecl,
    VarDecl,
    BType,
    ConstDef,
    Ident,
    ConstExp,
    ConstInitVal,
    VarDef,
    InitVal,
    Exp,
    FuncType,
    FuncFParams,
    Block,
    FuncFParam,
    BlockItem,
    Stmt,
    LVal,
    Cond,
    ForStmt,
    FormatString,
    AddExp,
    LOrExp,
    PrimaryExp,
    Number,
    IntConst,
    UnaryExp,
    UnaryOp,
    FuncRParams,
    MulExp,
    RelExp,
    EqExp,
    LAndExp,
    // 指代终结符
    EndNode
}
