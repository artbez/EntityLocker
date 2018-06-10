package iimetra.example.concurrent.test;

import iimetra.example.concurrent.lock.EntityLockerFactory;
import iimetra.example.concurrent.lock.locker.TimeoutEntityLocker;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.infra.results.III_Result;

import java.util.concurrent.TimeUnit;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest
@Description("Test concurrent modification protected code by timeout locking")
@Outcome(id = "1, 2, 3", expect = ACCEPTABLE, desc = "consequence increment")
@Outcome(id = "1, 3, 2", expect = ACCEPTABLE, desc = "consequence increment")
@Outcome(id = "2, 1, 3", expect = ACCEPTABLE, desc = "consequence increment")
@Outcome(id = "2, 3, 1", expect = ACCEPTABLE, desc = "consequence increment")
@Outcome(id = "3, 1, 2", expect = ACCEPTABLE, desc = "consequence increment")
@Outcome(id = "3, 2, 1", expect = ACCEPTABLE, desc = "consequence increment")
@Outcome(expect = FORBIDDEN, desc = "data race or too long incrementing")
public class ProtectionCodeTimeoutLockTest {

    private final TimeoutEntityLocker locker = EntityLockerFactory.Companion.createWithDefaultConfiguration();

    @Actor
    public void actor1(EntityState.SimpleState state, III_Result result) {
        Boolean success = locker.lock(30, TimeUnit.SECONDS, state.getEntity().getId());
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
    public void actor2(EntityState.SimpleState state, III_Result result) {
        Boolean success = locker.lock(30, TimeUnit.SECONDS, state.getEntity().getId());
        if (success) {
            try {
                state.getEntity().inc();
                result.r2 = state.getEntity().getCount();
            } finally {
                locker.unlock(state.getEntity().getId());
            }
        }
    }

    @Actor
    public void actor3(EntityState.SimpleState state, III_Result result) {
        locker.lock();
        try {
            state.getEntity().inc();
            result.r3 = state.getEntity().getCount();
        } finally {
            locker.unlock();
        }
    }
}
