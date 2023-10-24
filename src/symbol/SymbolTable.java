package symbol;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
    private final Map<String, Symbol> symbolMap = new HashMap<>();
    private final SymbolTable parent;

    public void addSymbol(Symbol symbol) {
        symbolMap.put(symbol.getToken().getValue(), symbol);
    }

    public SymbolTable(SymbolTable parent) {
        this.parent = parent;
    }

    public SymbolTable() {
        this.parent = null;
    }

    public Symbol getSymbolInAllLevel(String name) {
        SymbolTable table = this;
        while (table != null) {
            Symbol symbol = table.symbolMap.get(name);
            if (symbol != null) {
                return symbol;
            }
            table = table.parent;
        }
        return null;
    }

    public Symbol getSymbolInCurrentLevel(String name) {
        return symbolMap.get(name);
    }
}
