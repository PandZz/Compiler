package token;

public class Token {
    private TokenType type;
    private String value;
    private int numValue;
    private int line;

    public Token(TokenType type, String value, int line) {
        this.type = type;
        this.value = value;
        this.line = line;
    }

    public Token(TokenType type, int numValue, int line) {
        this.type = type;
        this.numValue = numValue;
        this.line = line;
        this.value = String.valueOf(numValue);
    }

    public int getNumValue() {
        return numValue;
    }

    public void setNumValue(int numValue) {
        this.numValue = numValue;
    }

    public TokenType getType() {
        return type;
    }

    public void setType(TokenType type) {
        this.type = type;
    }

    /**
     * 获取 Token 的值
     * @return (String) Token 的值, 形如 int, getint, {, "hello world"等
     */
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    @Override
    public String toString() {
        return String.format("%s %s", type, value);
    }

    public String toDebugString() {
        return String.format("[type]:%s [value]:%s [line]:%d", type, value, line);
    }
}
