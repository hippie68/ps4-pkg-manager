import java.util.LinkedList;

public class PKGQueue<T> {
    private LinkedList<T> queue = new LinkedList<T>();

    public synchronized void push(T o) {
        queue.add(o);
        this.notifyAll();
    }

    public synchronized void push(T[] o) {
        for (T e : o)
            queue.add(e);
        this.notifyAll();
    }

    public synchronized T pop() {
        while (queue.size() == 0)
            try {
                this.wait();
            } catch (InterruptedException ignore) {
            }
        return queue.remove();
    }

    public synchronized int getLength() {
        return queue.size();
    }
}
