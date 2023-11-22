package ir.type;

public class LabelType implements Type {
    private static int HANDLER = -1;
    private final int handler;

    public LabelType() {
        this.handler = ++LabelType.HANDLER;
    }

    public int getHandler() {
        return handler;
    }

    @Override
    public String toString() {
        return "label_" + handler;
    }
}
