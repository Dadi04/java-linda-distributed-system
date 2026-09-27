package linda;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TupleSpace {
    private final List<Tuple> tuples = new ArrayList<>();

    public synchronized void out(String[] tupleArray) {
        if (tupleArray == null) {
            throw new IllegalArgumentException("Tuple cannot be null");
        }
        for (String s : tupleArray) {
            if (s == null) {
                throw new IllegalArgumentException("Tuple fields during out operation cannot be null");
            }
        }

        Tuple tuple = new Tuple(tupleArray);
        tuples.add(tuple);

        notifyAll();
    }

    public synchronized void in(String[] template) {
        if (template == null) {
            throw new IllegalArgumentException("Template cannot be null");
        }

        while (true) {
            Iterator<Tuple> iterator = tuples.iterator();
            while (iterator.hasNext()) {
                Tuple t = iterator.next();
                if (t.matches(template)) {
                    t.fillTemplate(template);
                    iterator.remove();
                    return;
                }
            }

            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public synchronized boolean inp(String[] template) {
        if (template == null) return false;

        Iterator<Tuple> iterator = tuples.iterator();
        while (iterator.hasNext()) {
            Tuple t = iterator.next();
            if (t.matches(template)) {
                t.fillTemplate(template);
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    public synchronized void rd(String[] template) {
        if (template == null) {
            throw new IllegalArgumentException("Template cannot be null");
        }

        while (true) {
            for (Tuple t : tuples) {
                if (t.matches(template)) {
                    t.fillTemplate(template);
                    return;
                }
            }

            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public synchronized boolean rdp(String[] template) {
        if (template == null) return false;

        for (Tuple t : tuples) {
            if (t.matches(template)) {
                t.fillTemplate(template);
                return true;
            }
        }
        return false;
    }

    public synchronized int size() {
        return tuples.size();
    }
}
