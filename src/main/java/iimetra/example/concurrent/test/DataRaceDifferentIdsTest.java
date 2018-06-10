package iimetra.example.concurrent.test;

import iimetra.example.concurrent.lock.EntityLockerFactory;
import iimetra.example.concurrent.lock.locker.TimeoutEntityLocker;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest
@Description("Parallel working with two entities")
@Outcome(id = "1, 1", expect = ACCEPTABLE, desc = "correct increment")
@Outcome(expect = FORBIDDEN, desc = "data race")
public class DataRaceDifferentIdsTest {

    private final TimeoutEntityLocker locker = EntityLockerFactory.Companion.createWithDefaultConfiguration();

    @Actor
    public void actor1(EntityState.EntityPair state, II_Result result) {
        locker.lock(state.getEntity1().getId());
        try {
            state.getEntity1().inc();
            result.r1 = state.getEntity1().getCount();
        } finally {
            locker.unlock(state.getEntity1().getId());
        }
    }

    @Actor
    public void actor2(EntityState.EntityPair state, II_Result result) {
        locker.lock(state.getEntity2().getId());
        try {
            state.getEntity2().inc();
            result.r2 = state.getEntity2().getCount();
        } finally {
            locker.unlock(state.getEntity2().getId());
        }
    }
}

