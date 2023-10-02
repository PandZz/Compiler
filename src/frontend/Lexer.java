package frontend;

import token.Token;
import token.TokenType;

import java.util.*;

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

    private final Map<String, TokenType> simpleTokens = new HashMap<>() {{
        put("!=", TokenType.NEQ);
        put("==", TokenType.EQL);
        put("<=", TokenType.LEQ);
        put(">=", TokenType.GEQ);
        put("!", TokenType.NOT);
        put("&&", TokenType.AND);
        put("||", TokenType.OR);
        put("+", TokenType.PLUS);
        put("-", TokenType.MINU);
        put("*", TokenType.MULT);
        put("/", TokenType.DIV);
        put("%", TokenType.MOD);
        put("<", TokenType.LSS);
        put(">", TokenType.GRE);
        put("=", TokenType.ASSIGN);
        put(";", TokenType.SEMICN);
        put(",", TokenType.COMMA);
        put("(", TokenType.LPARENT);
        put(")", TokenType.RPARENT);
        put("[", TokenType.LBRACK);
        put("]", TokenType.RBRACK);
        put("{", TokenType.LBRACE);
        put("}", TokenType.RBRACE);
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
        // /或注释, 在此就将注释完全跳过, 并返回有意义的下一个token
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
        // 简单符号
        else if (simpleTokens.containsKey(String.valueOf(c)) || simpleTokens.containsKey(String.valueOf(c) + nextC)) {
            String s = String.valueOf(c);
            if (simpleTokens.containsKey(s + nextC)) {
                s += nextC;
                curPos++;
            }
            Token token = new Token(simpleTokens.get(s), s, curLine);
            tokens.add(token);
            return token;
        }
        return null;
    }
}
