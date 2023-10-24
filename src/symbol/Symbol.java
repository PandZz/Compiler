package symbol;

import token.Token;

public class Symbol {
    private int id;
    private int tableId;
    private Token token;
    private SymbolType type;
    private boolean isConst;

    public Symbol(int id, int tableId, Token token, SymbolType type, boolean isConst) {
        this.id = id;
        this.tableId = tableId;
        this.token = token;
        this.type = type;
        this.isConst = isConst;
    }

    public Symbol(Token token, SymbolType type, boolean isConst) {
        this.token = token;
        this.type = type;
        this.isConst = isConst;
    }

    public Token getToken() {
        return token;
    }
}
