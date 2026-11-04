package team.unnamed.mocha.runtime.value;

import team.unnamed.mocha.runtime.ExecutionContext;

/** Explicit eager numeric host ABI. Lazy/general functions keep the normal Arguments API. */
public interface NumericFunction extends Function<Object> {
    int arity();
    double invoke(double a, double b, double c);

    @Override
    default Value evaluate(ExecutionContext<Object> context, Arguments arguments) {
        int count=arity();
        if(count<0||count>3)throw new IllegalArgumentException("NumericFunction arity must be 0..3");
        double a=count>0?arguments.next().eval().getAsNumber():0;
        double b=count>1?arguments.next().eval().getAsNumber():0;
        double c=count>2?arguments.next().eval().getAsNumber():0;
        return NumberValue.of(invoke(a,b,c));
    }
}
