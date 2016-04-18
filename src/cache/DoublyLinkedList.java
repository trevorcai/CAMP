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
class ListNode<T> {
    public T value;
    ListNode<T> prev, next;

    public ListNode(T value) {
        this.value = value;
        this.prev = null;
        this.next = null;
    }
}
