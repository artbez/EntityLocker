package iimetra.example.concurrent.test;


import iimetra.example.concurrent.lock.EntityLocker;
import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Description;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

@JCStressTest
@Description("Test concurrent modification protected code")
@Outcome(id = "1, 2", expect = ACCEPTABLE, desc = "consequence increment")
@Outcome(id = "2, 1", expect = ACCEPTABLE, desc = "consequence increment")
@Outcome(expect = FORBIDDEN, desc = "data race")
public class ModifyTest {

    @Actor
    public void actor1(EntityState.SimpleEntity state, II_Result result) {
        EntityLocker.Companion.lock(state.getId());
        try {
            state.inc();
            result.r1 = state.getCount();
        } finally {
            EntityLocker.Companion.unlock(state.getId());
        }
    }

    @Actor
    public void actor2(EntityState.SimpleEntity state, II_Result result) {
        EntityLocker.Companion.lock(state.getId());
        try {
            state.inc();
            result.r2 = state.getCount();
        } finally {
            EntityLocker.Companion.unlock(state.getId());
        }
    }
}
