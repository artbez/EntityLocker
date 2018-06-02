package iimetra.example.concurrent.test;

public class SimpleEntity {
    private final long id;
    private int count = 0;

    public SimpleEntity(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public int getCount() {
        return count;
    }

    public void inc() {
        count++;
    }
}

