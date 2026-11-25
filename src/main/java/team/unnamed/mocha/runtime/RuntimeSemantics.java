package team.unnamed.mocha.runtime;

import team.unnamed.mocha.runtime.value.*;

/** Value rules for interpreted execution; never captures a host. */
public final class RuntimeSemantics {
    private RuntimeSemantics() {}
    public static Value lookup(Scope scope,String name) {
        Value result=value(scope.get(name));
        if(!result.isNil())return result;
        String alias=name.equals("v")?"variable":name.equals("variable")?"v":name.equals("q")?"query":name.equals("query")?"q":name.equals("t")?"temp":name.equals("temp")?"t":name.equals("c")?"context":name.equals("context")?"c":name;
        return alias.equals(name)?result:value(scope.get(alias));
    }
    public static Object actor(Value value) {
        if(value instanceof ActorReference)return ((ActorReference)value).valid()?((ActorReference)value).entity():null;
        return value instanceof JavaValue?((JavaValue)value).value():null;
    }
    public static Scope actorScope(Value value,Scope fallback) {
        return value instanceof ActorReference?((ActorReference)value).bindings():fallback;
    }
    public static Value value(Value value) { return value == null ? Value.nil() : value; }
    public static Value property(Value object, String key) {
        return object instanceof ObjectValue ? value(((ObjectValue) object).get(key)) : Value.nil();
    }
    public static Value array(Value object, Value index) {
        if (!(object instanceof ArrayValue)) return Value.nil();
        Value[] values = ((ArrayValue) object).values();
        return values.length == 0 ? Value.nil() : value(values[Math.max(0, (int) index.getAsNumber()) % values.length]);
    }
    public static boolean equal(Value a, Value b) {
        return a.isString() || b.isString() ? a.getAsString().equals(b.getAsString()) : a.getAsNumber() == b.getAsNumber();
    }
    public static double divide(double a, double b) { return b == 0 ? 0 : a / b; }
    public static int loopCount(double n) { return Math.max(0, Math.min(1024, Math.round((float)n))); }
    public static Value assign(Value object, String key, Value value) {
        if (object instanceof MutableObjectBinding) ((MutableObjectBinding)object).set(key, value);
        return value;
    }
    /** Materialize only absent struct parents, and respect a blocked/read-only binding. */
    public static Value struct(Value object, String key) {
        Value child = property(object, key);
        if (child.isNil() && object instanceof MutableObjectBinding) {
            MutableObjectBinding created = new MutableObjectBinding();
            if (((MutableObjectBinding)object).set(key, created)) return created;
        }
        return child;
    }
}
