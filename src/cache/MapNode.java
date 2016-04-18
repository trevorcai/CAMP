package cache;

/* Struct-like construction of Nodes within Map */
class MapNode {
    public final String value;
    public int cost;
    public ListNode<String> node;

    public MapNode(String value, int cost, ListNode node) {
        this.value = value;
        this.cost = cost;
        this.node = node;
    }

    public int getSize() {
        return value.length();
    }
}