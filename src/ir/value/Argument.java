package ir.value;

import ir.type.PointerType;
import ir.type.Type;

public class Argument extends User{
    public Argument(Type type) {
        // 这里的名称自动生成(因此其name只是参数的一个标识符, 而与源程序定义的变量无关)
        super("%" + (++valNumber), type);
    }
}
