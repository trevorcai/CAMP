package cache;

/* Struct-like construction of Nodes within Map */
class MapNode {
    public final String value;
    public final int cost;
    private final int size;
    public ListNode<String> node;

    public MapNode(String value, int cost, int size, ListNode node) {
        this.value = value;
        this.cost = cost;
        this.size = size;
        this.node = node;
    }

    public int getSize() {
        return size;
    }
}