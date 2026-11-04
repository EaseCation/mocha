package team.unnamed.mocha.runtime.value;

import team.unnamed.mocha.runtime.Scope;

/** Host-owned actor access. The host exposes only permitted variables/queries and invalidates stale references. */
public interface ActorReference extends Value {
    Object entity();
    Scope bindings();
    boolean valid();
    @Override default boolean isNil() { return !valid(); }
}
