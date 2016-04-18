package cache;

/* This class is NOT thread-safe! Any concurrent accesses must be controlled by
   external synchronization.
 */
public class DoublyLinkedList<E> {
    private ListNode<E> head, tail;

    public DoublyLinkedList() {
        head = null;
        tail = null;
    }

    public ListNode<E> peekHead() {
        return head;
    }

    public ListNode<E> popHead() {
        if (head == null) {
            return null;
        }
        ListNode<E> prevHead = head;
        head = prevHead.next;
        if (head != null) {
            head.prev = null;
        } else {
            tail = null;
        }
        prevHead.next = null;

        return prevHead;
    }

    public void pushTail(final ListNode<E> e) {
        ListNode<E> prevTail = tail;
        tail = e;

        e.prev = prevTail;
        e.next = null;
        if (prevTail == null) {
            head = e;
        } else {
            prevTail.next = e;
        }
    }

    public void remove(final ListNode<E> e) {
        ListNode<E> previous = e.prev;
        ListNode<E> next = e.next;

        if (previous != null) {
            previous.next = next;
            e.prev = null;
        } else {
            head = next;
        }

        if (next != null) {
            next.prev = previous;
            e.next = null;
        } else {
            tail = previous;
        }
    }

    public void moveTail(final ListNode<E> e) {
        // If already at end, don't bother with extra ops
        if (e == tail) {
            return;
        }
        remove(e);
        pushTail(e);
    }

    public boolean isHead(final ListNode<E> e) {
        return (e == head);
    }

    public boolean isEmpty() {
        return (head == null);
    }
}

/* Struct-like construction of Nodes within List */
class ListNode<T> implements Comparable<ListNode<T>> {
    public T value;
    private int ordering;
    ListNode<T> prev, next;

    public ListNode(T value) {
        this(value, 0);
    }

    public ListNode(T value, int ordering) {
        this.value = value;
        this.ordering = ordering;
        this.prev = null;
        this.next = null;
    }

    public int getOrdering() {
        return ordering;
    }

    public void setOrdering(int ordering) {
        this.ordering = ordering;
    }

    @Override
    public int compareTo(ListNode<T> other) {
        if (this.ordering < other.ordering) {
            return -1;
        } else if (this.ordering == other.ordering){
            return 0;
        } else {
            return 1;
        }
    }
}
