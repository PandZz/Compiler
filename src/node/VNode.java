package node;

import token.Token;
import utils.IOUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VNode {
    private List<VNode> childrenNodes;
    private NodeType nodeType;
    private String value;
    private int line; // 代表当前node最后一个token所在的行数

    private static final Set<NodeType> justifiedNodeTypes = Set.of(
            NodeType.MulExp, NodeType.AddExp, NodeType.RelExp, NodeType.EqExp, NodeType.LAndExp, NodeType.LOrExp
    );

    public VNode(List<VNode> childrenNodes, NodeType nodeType) {
        this.childrenNodes = new ArrayList<>(childrenNodes);
        this.nodeType = nodeType;
        this.value = '<' + nodeType.toString() + '>';
        this.line = getLastChildNode().getLine();
        // 以下节点类型不需要输出
        if (nodeType == NodeType.BlockItem || nodeType == NodeType.Decl || nodeType == NodeType.BType) {
            this.value = "";
        }
    }

    // 以Token创建的叶子节点都是终结符
    public VNode(Token token) {
        this.value = token.toString();
        this.nodeType = NodeType.EndNode;
        this.line = token.getLine();
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public void addChildNode(VNode childNode) {
        childrenNodes.add(childNode);
    }

    /**
     * 获取当前语法树节点在源文件中的行数
     * @return 行数(非终结符的行数为最后一个子节点的行数)
     */
    public int getLine() {
        return line;
    }

    public List<VNode> getChildrenNodes() {
        return childrenNodes;
    }

    /**
     * 获取当前语法树节点的第index个子节点
     * @param index 子节点的下标
     * @return 对应子结点对象, 下标超出则为null
     */
    public VNode getChildNode(int index) {
        if (index < 0 || index >= childrenNodes.size()) {
            return null;
        }
        return childrenNodes.get(index);
    }

    public VNode get1stChildNode() {
        return childrenNodes.get(0);
    }

    public VNode getLastChildNode() {
        return childrenNodes.get(childrenNodes.size() - 1);
    }

    /**
     * 获取语法树节点的值
     * @return 形如: &lt;Decl&gt; 或 SEMICON ; 的字符串
     */
    public String getValue() {
        return value;
    }

    public void printToBuffer() {
        if (childrenNodes != null) {
            for (VNode childNode : childrenNodes) {
                childNode.printToBuffer();
            }
        }
        if (!this.value.isEmpty()) {
            IOUtils.appendBuffer(this.value);
        }
    }
}
