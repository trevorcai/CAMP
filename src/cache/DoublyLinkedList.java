package cache;

/* This class is NOT thread-safe! Any concurrent accesses must be controlled by
   external synchronization.
 */
public class DoublyLinkedList<T extends ListNode<T>> {
    private T head, tail;

    public DoublyLinkedList() {
        head = null;
        tail = null;
    }

    public T peekHead() {
        return head;
    }

    public T popHead() {
        if (head == null) {
            return null;
        }
        T prevHead = head;
        head = prevHead.next;
        if (head != null) {
            head.prev = null;
        } else {
            tail = null;
        }
        prevHead.next = null;

        return prevHead;
    }

    public void pushTail(final T e) {
        T prevTail = tail;
        tail = e;

        e.prev = prevTail;
        e.next = null;
        if (prevTail == null) {
            head = e;
        } else {
            prevTail.next = e;
        }
    }

    public void remove(final T e) {
        T previous = e.prev;
        T next = e.next;

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

    public void moveTail(final T e) {
        // If already at end, don't bother with extra ops
        if (e == tail) {
            return;
        }
        remove(e);
        pushTail(e);
    }

    public boolean isHead(final T e) {
        return (e == head);
    }

    public boolean isEmpty() {
        return (head == null);
    }
}

/* Struct-like construction of Nodes within List */
abstract class ListNode<T extends ListNode<T>> {
    T prev, next;

    public ListNode() {
        this.prev = null;
        this.next = null;
    }

    public boolean isValid() {
        return (prev != null) || (next != null);
    }
}
