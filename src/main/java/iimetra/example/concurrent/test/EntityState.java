package iimetra.example.concurrent.test;


import org.openjdk.jcstress.annotations.State;

class EntityState {

    @State
    public static class SimpleEntity {
        private final long id = 12345;
        private int count = 0;

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
}
