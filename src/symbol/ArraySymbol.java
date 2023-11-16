package symbol;

public class ArraySymbol implements Symbol{
    private String name;
    /**
     * -1 for void, 0 for variable, 1 for array, 2 for matrix
     */
    private int dimension;
    /**
     * true for const, false for var
     */
    private boolean isConst;

    public ArraySymbol(String name, int dimension, boolean isConst) {
        this.name = name;
        this.dimension = dimension;
        this.isConst = isConst;
    }

    public String getName() {
        return name;
    }

    /**
     * 获取维度
     * @return 维度 (-1 for void, 0 for variable, 1 for array, 2 for matrix)
     */
    public int getDimension() {
        return dimension;
    }

    public boolean isConst() {
        return isConst;
    }

    @Override
    public boolean match(String name) {
        return this.name.equals(name);
    }
}
