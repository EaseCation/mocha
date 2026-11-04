package team.unnamed.mocha.runtime.program;

import team.unnamed.mocha.runtime.Scope;
import team.unnamed.mocha.runtime.value.Value;

/** Immutable JVM program. Bindings, entity references and evaluation state are supplied per call. */
public interface Program {
    /** Low-level sampling entry: caller owns temp, query, this and variable bindings. */
    Value evaluate(Object entity, Scope scope);
    /** High-level expression entry: fresh temp/t bindings for each evaluation. */
    Value evaluateFresh(Object entity, Scope scope);
    int nodeCount();
    int bytecodeSize();
    /** Defensive copy for diagnostics; never an executable resource input. */
    byte[] bytecode();
}
