package iimetra.example.concurrent.test;


import org.openjdk.jcstress.annotations.State;

public class EntityState {

    @State
    public static class SimpleState {
        private final SimpleEntity entity = new SimpleEntity(0);

        public SimpleEntity getEntity() {
            return entity;
        }
    }

    @State
    public static class EntityPair {
        private final SimpleEntity entity1 = new SimpleEntity(0);
        private final SimpleEntity entity2 = new SimpleEntity(1);

        public SimpleEntity getEntity1() {
            return entity1;
        }

        public SimpleEntity getEntity2() {
            return entity2;
        }
    }
}