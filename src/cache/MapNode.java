package cache;

/* Struct-like construction of Nodes within Map */
class MapNode extends ListNode<MapNode> implements Comparable<MapNode> {
    private final String key, value;
    private final int cost, size;
    private int ordering = 0;
    private boolean evicted = false;

    public MapNode(String key, String value, int cost, int size) {
        super();
        this.key = key;
        this.value = value;
        this.cost = cost;
        this.size = size;
    }


    public boolean isEvicted() {
        return evicted;
    }

    public void setEvicted() {
        evicted = true;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public int getCost() {
        return cost;
    }

    public int getSize() {
        return size;
    }

    public int getOrdering() {
        return ordering;
    }

    public void setOrdering(int ordering) {
        this.ordering = ordering;
    }

    @Override
    public int compareTo(MapNode other) {
        if (this.ordering < other.ordering) {
            return -1;
        } else if (this.ordering == other.ordering){
            return 0;
        } else {
            return 1;
        }
    }
}