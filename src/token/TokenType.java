package token;

public enum TokenType {
    IDENFR, // 标识符
    INTCON, // 整数常量，如 123
    STRCON, // 字符串常量，如 "hello world"
    MAINTK,
    CONSTTK,
    INTTK,
    BREAKTK,
    CONTINUETK,
    IFTK,
    ELSETK,
    NOT,
    AND,
    OR,
    FORTK,
    GETINTTK,
    PRINTFTK,
    RETURNTK,
    PLUS,
    MINU,
    VOIDTK,
    MULT,
    DIV,
    MOD,
    LSS,
    LEQ,
    GRE,
    GEQ,
    EQL,
    NEQ,
    ASSIGN, // 赋值符号
    SEMICN, // 分号
    COMMA, // 逗号
    LPARENT, // 左括号
    RPARENT, // 右括号
    LBRACK, // 左中括号
    RBRACK, // 右中括号
    LBRACE, // 左大括号
    RBRACE, // 右大括号
}
