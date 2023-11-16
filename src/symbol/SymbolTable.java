package symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {
    private SymbolTable parentTable;
    private Map<String, Symbol> symbolMap;
    private List<SymbolTable> childrenTables;

    public SymbolTable(SymbolTable parentTable) {
        this.parentTable = parentTable;
        symbolMap = new HashMap<>();
        childrenTables = new ArrayList<>();
    }

    public SymbolTable getParentTable() {
        return parentTable;
    }

    public void addChildTable(SymbolTable childTable) {
        childrenTables.add(childTable);
    }

    public void addSymbol(Symbol symbol) {
        symbolMap.put(symbol.getName(), symbol);
    }

    public Symbol getSymbolByName(String name, boolean recursive) {
        Symbol symbol = symbolMap.get(name);
        if (recursive && symbol == null && parentTable != null) {
            return parentTable.getSymbolByName(name, true);
        }
        return symbol;
    }
}
