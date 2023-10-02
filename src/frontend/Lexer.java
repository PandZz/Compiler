package frontend;

import token.Token;
import token.TokenType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {
    private Lexer() {}

    private static Lexer instance = null;

    public static Lexer getInstance() {
        if (instance == null) {
            instance = new Lexer();
        }
        return instance;
    }

    private final List<Token> tokens = new ArrayList<>();

    private final Map<String, TokenType> reserveWords = new HashMap<>() {{
        put("main", TokenType.MAINTK);
        put("const", TokenType.CONSTTK);
        put("int", TokenType.INTTK);
        put("break", TokenType.BREAKTK);
        put("continue", TokenType.CONTINUETK);
        put("if", TokenType.IFTK);
        put("else", TokenType.ELSETK);
        put("for", TokenType.FORTK);
        put("getint", TokenType.GETINTTK);
        put("printf", TokenType.PRINTFTK);
        put("return", TokenType.RETURNTK);
        put("void", TokenType.VOIDTK);
    }};

    private String source = null;
    private int sourceLength = 0;
    private int curPos = -1;
    private int curLine = 1;

    public void setSource(String source) {
        this.source = source;
        this.sourceLength = source.length();
    }

    public List<Token> getTokens() {
        return tokens;
    }

    /**
     * 获取下一个token并将其加入到tokens中
     * @return 下一个token(如果没有则返回null)
     */
    public Token next() {
        char c;
        // 自动往下一个字符移动一位并将空白字符跳过
        do {
            if (curPos + 1 >= sourceLength) return null;
            c = source.charAt(++curPos);
            if (c == '\n') curLine++;
        } while (Character.isWhitespace(c));
        char nextC = curPos + 1 < sourceLength ? source.charAt(curPos + 1) : '\0';
        // 标识符或保留字
        if (Character.isLetter(c) || c == '_') {
            String s = "";
            for (int j = curPos; j < sourceLength; j++) {
                char d = source.charAt(j);
                if (d == '_' || Character.isLetter(d) || Character.isDigit(d)) s += d;
                else {
                    curPos = j - 1;
                    break;
                }
            }
            Token token = new Token(reserveWords.getOrDefault(s, TokenType.IDENFR), s, curLine);
            tokens.add(token);
            return token;
        }
        // 无符号整数
        else if (Character.isDigit(c)) {
            String s = "";
            for (int j = curPos; j < sourceLength; j++) {
                char d = source.charAt(j);
                if (Character.isDigit(d)) s += d;
                else {
                    curPos = j - 1;
                    break;
                }
            }
            Token token = new Token(TokenType.INTCON, Integer.parseInt(s), curLine);
            tokens.add(token);
            return token;
        }
        // 字符串常量
        else if (c == '\"') {
            String s = "\"";
            for (int j = curPos + 1; j < sourceLength; j++) {
                char d = source.charAt(j);
                if (d != '\"') {
                    s += d;
                } else {
                    curPos = j;
                    s += "\"";
                    break;
                }
            }
            Token token = new Token(TokenType.STRCON, s, curLine);
            tokens.add(token);
            return token;
        }
        // !, !=
        else if (c == '!') {
            Token token;
            if (nextC == '=') {
                curPos++;
                token = new Token(TokenType.NEQ, "!=", curLine);
            } else token = new Token(TokenType.NOT, "!", curLine);
            tokens.add(token);
            return token;
        }
        // &&
        else if (c == '&') {
            if (nextC == '&') {
                curPos++;
                Token token = new Token(TokenType.AND, "&&", curLine);
                tokens.add(token);
                return token;
            }
        }
        // ||
        else if (c == '|') {
            if (nextC == '|') {
                curPos++;
                Token token = new Token(TokenType.OR, "||", curLine);
                tokens.add(token);
                return token;
            }
        }
        // <, <=
        else if (c == '<') {
            Token token;
            if (nextC == '=') {
                curPos++;
                token = new Token(TokenType.LEQ, "<=", curLine);
            } else token = new Token(TokenType.LSS, "<", curLine);
            tokens.add(token);
            return token;
        }
        // >, >=
        else if (c == '>') {
            Token token;
            if (nextC == '=') {
                curPos++;
                token = new Token(TokenType.GEQ, ">=", curLine);
            } else token = new Token(TokenType.GRE, ">", curLine);
            tokens.add(token);
            return token;
        }
        // =, ==
        else if (c == '=') {
            Token token;
            if (nextC == '=') {
                curPos++;
                token = new Token(TokenType.EQL, "==", curLine);
            } else token = new Token(TokenType.ASSIGN, "=", curLine);
            tokens.add(token);
            return token;
        }
        // +
        else if (c == '+') {
            Token token = new Token(TokenType.PLUS, "+", curLine);
            tokens.add(token);
            return token;
        }
        // -
        else if (c == '-') {
            Token token = new Token(TokenType.MINU, "-", curLine);
            tokens.add(token);
            return token;
        }
        // *
        else if (c == '*') {
            Token token = new Token(TokenType.MULT, "*", curLine);
            tokens.add(token);
            return token;
        }
        // /或者是注释, 在此就将注释完全跳过, 并返回有意义的下一个token
        else if (c == '/') {
            if (nextC == '/') {
                int j = source.indexOf('\n', curPos + 2);
                if (j != -1) curPos = j - 1;
                else curPos = sourceLength - 1;
                return next();
            } else if (nextC == '*') {
                for (int j = curPos + 2; j < sourceLength; j++) {
                    char e = source.charAt(j);
                    if (e == '\n') curLine++;
                    else if (e == '*' && source.charAt(j + 1) == '/') {
                        curPos = j + 1;
                        break;
                    }
                }
                return next();
            } else {
                Token token = new Token(TokenType.DIV, "/", curLine);
                tokens.add(token);
                return token;
            }
        }
        // %
        else if (c == '%') {
            Token token = new Token(TokenType.MOD, "%", curLine);
            tokens.add(token);
            return token;
        }
        // ,
        else if (c == ',') {
            Token token = new Token(TokenType.COMMA, ",", curLine);
            tokens.add(token);
            return token;
        }
        // ;
        else if (c == ';') {
            Token token = new Token(TokenType.SEMICN, ";", curLine);
            tokens.add(token);
            return token;
        }
        // (
        else if (c == '(') {
            Token token = new Token(TokenType.LPARENT, "(", curLine);
            tokens.add(token);
            return token;
        }
        // )
        else if (c == ')') {
            Token token = new Token(TokenType.RPARENT, ")", curLine);
            tokens.add(token);
            return token;
        }
        // [
        else if (c == '[') {
            Token token = new Token(TokenType.LBRACK, "[", curLine);
            tokens.add(token);
            return token;
        }
        // ]
        else if (c == ']') {
            Token token = new Token(TokenType.RBRACK, "]", curLine);
            tokens.add(token);
            return token;
        }
        // {
        else if (c == '{') {
            Token token = new Token(TokenType.LBRACE, "{", curLine);
            tokens.add(token);
            return token;
        }
        // }
        else if (c == '}') {
            Token token = new Token(TokenType.RBRACE, "}", curLine);
            tokens.add(token);
            return token;
        }
        return null;
    }
}
