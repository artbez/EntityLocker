## EntityLocker

1. EntityLocker should support different types of entity IDs.

2. EntityLocker’s interface should allow the caller to specify which entity does it want to work with (using entity ID), and designate the boundaries of the code that should have exclusive access to the entity (called “protected code”).

3. For any given entity, EntityLocker should guarantee that at most one thread executes protected code on that entity. If there’s a concurrent request to lock the same entity, the other thread should wait until the entity becomes available.

4. EntityLocker should allow concurrent execution of protected code on different entities.


### Also released:

I. Allow reentrant locking (not for global).

II. Allow the caller to specify timeout for locking an entity (not for global).

III. Implement protection from deadlocks (but not taking into account possible locks outside EntityLocker).

IV. Implement global lock. Protected code that executes under a global lock must not execute concurrently with any other protected code.


### Tests
'gradle jcstress' to jcstress tests run

'geadle test' to unit tests run
