package iimetra.example.concurrent.test;

import iimetra.example.concurrent.lock.EntityLockerFactory;
import iimetra.example.concurrent.lock.locker.TimeoutEntityLocker;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.infra.results.II_Result;

import java.util.concurrent.TimeUnit;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest
@Description("Test concurrent modification protected code")
@Outcome(id = "1, 2", expect = ACCEPTABLE, desc = "consequence increment")
@Outcome(id = "2, 1", expect = ACCEPTABLE, desc = "consequence increment")
@Outcome(expect = FORBIDDEN, desc = "data race or too long incrementing")
public class ProtectionCodeTimeoutLockTest {

    private final TimeoutEntityLocker locker = EntityLockerFactory.Companion.createFull();

    @Actor
    public void actor1(EntityState.SimpleState state, II_Result result) {
        Boolean success = locker.lock(10, TimeUnit.SECONDS, state.getEntity().getId());
        if (success) {
            try {
                state.getEntity().inc();
                result.r1 = state.getEntity().getCount();
            } finally {
                locker.unlock(state.getEntity().getId());
            }
        }
    }

    @Actor
    public void actor2(EntityState.SimpleState state, II_Result result) {
        Boolean success = locker.lock(10, TimeUnit.SECONDS, state.getEntity().getId());
        if (success) {
            try {
                state.getEntity().inc();
                result.r2 = state.getEntity().getCount();
            } finally {
                locker.unlock(state.getEntity().getId());
            }
        }
    }
}
