import java.util.LinkedList;

public class PkgQueue<T> {
    private LinkedList<T> queue = new LinkedList<T>();

    public synchronized void push(T o) {
        queue.add(o);
        this.notifyAll();
    }

    public synchronized void push(T[] o) {
        if (o == null)
            queue.add(null);
        else
            for (T e : o)
                queue.add(e);
        this.notifyAll();
    }

    public synchronized T pop() throws InterruptedException {
        while (queue.size() == 0)
            this.wait();
        return queue.remove();
    }

    public synchronized int getLength() {
        return queue.size();
    }
}
