package symbol;

import java.util.List;

public class FuncSymbol implements Symbol{
    private String name;
    private FuncType type;
    private List<ArraySymbol> params;

    public FuncSymbol(String name, FuncType type, List<ArraySymbol> params) {
        this.name = name;
        this.type = type;
        this.params = params;
    }

    public String getName() {
        return name;
    }

    public FuncType getType() {
        return type;
    }

    public List<ArraySymbol> getParams() {
        return params;
    }

    @Override
    public boolean match(String name) {
        return this.name.equals(name);
    }
}
