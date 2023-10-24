import node.NodeType;
import node.VNode;

import java.util.ArrayList;
import java.util.List;

public class Test {
    private boolean ERROR = true;
    public VNode testFunc() {
        if (ERROR) {
            return null;
        }
        return new VNode(null, null, NodeType.FuncType);
    }
    public static void main(String[] args) {
        Test test = new Test();
        List<VNode> nodeList = new ArrayList<>();
        nodeList.add(test.testFunc());
        System.out.println(nodeList.size());
    }
}
