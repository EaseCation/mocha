/*
 * This file is part of mocha, licensed under the MIT license
 *
 * Copyright (c) 2021-2025 Unnamed Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package team.unnamed.mocha.runtime.standard;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.unnamed.mocha.runtime.binding.BindExternalFunction;
import team.unnamed.mocha.runtime.binding.Binding;
import team.unnamed.mocha.runtime.value.NumberValue;
import team.unnamed.mocha.runtime.value.ObjectProperty;
import team.unnamed.mocha.runtime.value.ObjectValue;
import team.unnamed.mocha.util.CaseInsensitiveStringHashMap;

import java.util.Map;
import java.util.Random;

/**
 * Math function bindings inside an object
 * binding, commonly named 'math'
 */
@Binding("math")
@BindExternalFunction(at = Math.class, name = "abs", args = {double.class}, pure = true)
@BindExternalFunction(at = Math.class, name = "log", args = {double.class}, as = "ln", pure = true)
@BindExternalFunction(at = Math.class, name = "max", args = {double.class, double.class}, pure = true)
@BindExternalFunction(at = Math.class, name = "min", args = {double.class, double.class}, pure = true)
@BindExternalFunction(at = Math.class, name = "round", args = {double.class}, pure = true)
@BindExternalFunction(at = Math.class, name = "sqrt", args = {double.class}, pure = true)
@BindExternalFunction(at = Math.class, name = "pow", args = {double.class, double.class}, pure = true)
@BindExternalFunction(at = Math.class, name = "exp", args = {double.class}, pure = true)
@BindExternalFunction(at = Math.class, name = "floor", args = {double.class}, pure = true)
@BindExternalFunction(at = Math.class, name = "ceil", args = {double.class}, pure = true)
public final class MochaMath implements ObjectValue {
    @Binding("pi")
    public static final double PI = Math.PI;

    private static final double RADIAN = Math.toRadians(1);

    private static final Random RANDOM = new Random();

    private final Map<String, ObjectProperty> entries = new CaseInsensitiveStringHashMap<>();

    public MochaMath() {
        register("abs", Math::abs);
        register("acos", MochaMath::acos);
        register("asin", MochaMath::asin);
        register("atan", MochaMath::atan);
        register("atan2", MochaMath::atan2);
        register("ceil", Math::ceil);
        register("clamp", MochaMath::clamp);
        register("cos", MochaMath::cos);
        register("die_roll", MochaMath::dieRoll);
        register("die_roll_integer", MochaMath::dieRollInteger);
        register("exp", Math::exp);
        register("floor", Math::floor);
        register("hermite_blend", MochaMath::hermiteBlend);
        register("lerp", MochaMath::lerp);
        register("lerprotate", MochaMath::lerpRotate);
        register("ln", Math::log);
        register("max", Math::max);
        register("min", Math::min);
        register("min_angle", MochaMath::minAngle);
        register("mod", MochaMath::mod);
        entries.put("pi", ObjectProperty.property(NumberValue.of(Math.PI), true));
        register("pow", Math::pow);
        register("random", MochaMath::random);
        register("random_integer", MochaMath::randomInteger);
        register("round", Math::round);
        register("sin", MochaMath::sin);
        register("sqrt", Math::sqrt);
        register("trunc", MochaMath::trunc);

        register("copy_sign", MochaMath::copySign);
        register("sign", MochaMath::sign);
        register("inverse_lerp", MochaMath::inverseLerp);
        register("ease_in_back", MochaMath::easeInBack);
        register("ease_out_back", MochaMath::easeOutBack);
        register("ease_in_out_back", MochaMath::easeInOutBack);
        register("ease_in_bounce", MochaMath::easeInBounce);
        register("ease_out_bounce", MochaMath::easeOutBounce);
        register("ease_in_out_bounce", MochaMath::easeInOutBounce);
        register("ease_in_circ", MochaMath::easeInCirc);
        register("ease_out_circ", MochaMath::easeOutCirc);
        register("ease_in_out_circ", MochaMath::easeInOutCirc);
        register("ease_in_cubic", MochaMath::easeInCubic);
        register("ease_out_cubic", MochaMath::easeOutCubic);
        register("ease_in_out_cubic", MochaMath::easeInOutCubic);
        register("ease_in_elastic", MochaMath::easeInElastic);
        register("ease_out_elastic", MochaMath::easeOutElastic);
        register("ease_in_out_elastic", MochaMath::easeInOutElastic);
        register("ease_in_expo", MochaMath::easeInExpo);
        register("ease_out_expo", MochaMath::easeOutExpo);
        register("ease_in_out_expo", MochaMath::easeInOutExpo);
        register("ease_in_quad", MochaMath::easeInQuad);
        register("ease_out_quad", MochaMath::easeOutQuad);
        register("ease_in_out_quad", MochaMath::easeInOutQuad);
        register("ease_in_quart", MochaMath::easeInQuart);
        register("ease_out_quart", MochaMath::easeOutQuart);
        register("ease_in_out_quart", MochaMath::easeInOutQuart);
        register("ease_in_quint", MochaMath::easeInQuint);
        register("ease_out_quint", MochaMath::easeOutQuint);
        register("ease_in_out_quint", MochaMath::easeInOutQuint);
        register("ease_in_sine", MochaMath::easeInSine);
        register("ease_out_sine", MochaMath::easeOutSine);
        register("ease_in_out_sine", MochaMath::easeInOutSine);
        // all of our properties are constant
        entries.replaceAll((key, property) -> ObjectProperty.property(property.value(), true));
    }

    @Binding(value = "acos", skipChecking = true, pure = true)
    public static double acos(final double value) {
        return NumberValue.normalize(Math.acos(value) / RADIAN);
    }

    @Binding(value = "asin", skipChecking = true, pure = true)
    public static double asin(final double value) {
        return NumberValue.normalize(Math.asin(value) / RADIAN);
    }

    @Binding(value = "atan", pure = true)
    public static double atan(final double value) {
        return Math.atan(value) / RADIAN;
    }

    @Binding(value = "atan2", pure = true)
    public static double atan2(final double y, final double x) {
        return Math.atan2(y, x) / RADIAN;
    }

    @Binding(value = "clamp", pure = true)
    public static double clamp(final double value, final double min, final double max) {
        return Math.max(Math.min(value, max), min);
    }

    @Binding(value = "cos", pure = true)
    public static double cos(final double value) {
        return Math.cos(value * RADIAN);
    }

    @Binding("die_roll")
    public static double dieRoll(double amount, double low, double high) {
        double result = 0;
        for (int i=0;i<Math.max(0,Math.min(1024,(int)amount));i++) result += random(low,high);
        return result;
    }
    @Binding("die_roll_integer")
    public static double dieRollInteger(double amount, double low, double high) {
        double result = 0;
        for (int i=0;i<Math.max(0,Math.min(1024,(int)amount));i++) result += randomInteger(low,high);
        return result;
    }

    @Binding(value = "hermite_blend", pure = true)
    public static double hermiteBlend(final double t) {
        final double t2 = t * t;
        final double t3 = t2 * t;
        return 3 * t2 - 2 * t3;
    }

    @Binding(value = "lerp", pure = true)
    public static double lerp(final double start, final double end, final double lerp) {
        return start + lerp * (end - start);
    }

    @Binding(value = "lerprotate", pure = true)
    public static double lerpRotate(double start, double end, double weight) {
        return start + minAngle(end-start) * weight;
    }
    @Binding(value = "min_angle", pure = true)
    public static double minAngle(double angle) {
        if (!Double.isFinite(angle)) return 0;
        return ((angle + 180) % 360 + 360) % 360 - 180;
    }
    @Binding(value = "mod", pure = true)
    public static double mod(double a, double b) { return b==0 ? 0 : a%b; }
    @Binding("random")
    public static double random(double min, double max) {
        if (min > max) { double t=min;min=max;max=t; }
        if (min==max) return min;
        // Finite discrete uniform grid, including both documented endpoints.
        return min + (max-min)*(RANDOM.nextInt(16777217)/16777216.0);
    }
    @Binding("random_integer")
    public static int randomInteger(double min, double max) {
        int low=(int)Math.ceil(Math.min(min,max)),high=(int)Math.floor(Math.max(min,max));
        if(high<=low)return low;
        long span=(long)high-low+1, bits, remainder;
        do { bits=RANDOM.nextLong()>>>1;remainder=bits%span; } while(bits-remainder+(span-1)<0);
        return (int)(low+remainder);
    }
    @Binding(value="copy_sign",pure=true)
    public static double copySign(double a,double b) { return Math.copySign(a,b); }
    @Binding(value="sign",pure=true)
    public static double sign(double value) { return value>0?1:-1; }
    @Binding(value="inverse_lerp",pure=true)
    public static double inverseLerp(double start,double end,double value) { return end==start?0:(value-start)/(end-start); }

    @Binding(value = "sin", pure = true)
    public static double sin(final double value) {
        return Math.sin(value * RADIAN);
    }

    @Binding(value = "trunc", pure = true)
    public static double trunc(final double value) {
        return value - value % 1;
    }

    @Binding(value="ease_in_back",pure=true)
    public static double easeInBack(double start,double end,double t) { return lerp(start,end,ease("back",0,t)); }
    @Binding(value="ease_out_back",pure=true)
    public static double easeOutBack(double start,double end,double t) { return lerp(start,end,ease("back",1,t)); }
    @Binding(value="ease_in_out_back",pure=true)
    public static double easeInOutBack(double start,double end,double t) { return lerp(start,end,ease("back",2,t)); }
    @Binding(value="ease_in_bounce",pure=true)
    public static double easeInBounce(double start,double end,double t) { return lerp(start,end,ease("bounce",0,t)); }
    @Binding(value="ease_out_bounce",pure=true)
    public static double easeOutBounce(double start,double end,double t) { return lerp(start,end,ease("bounce",1,t)); }
    @Binding(value="ease_in_out_bounce",pure=true)
    public static double easeInOutBounce(double start,double end,double t) { return lerp(start,end,ease("bounce",2,t)); }
    @Binding(value="ease_in_circ",pure=true)
    public static double easeInCirc(double start,double end,double t) { return lerp(start,end,ease("circ",0,t)); }
    @Binding(value="ease_out_circ",pure=true)
    public static double easeOutCirc(double start,double end,double t) { return lerp(start,end,ease("circ",1,t)); }
    @Binding(value="ease_in_out_circ",pure=true)
    public static double easeInOutCirc(double start,double end,double t) { return lerp(start,end,ease("circ",2,t)); }
    @Binding(value="ease_in_cubic",pure=true)
    public static double easeInCubic(double start,double end,double t) { return lerp(start,end,ease("cubic",0,t)); }
    @Binding(value="ease_out_cubic",pure=true)
    public static double easeOutCubic(double start,double end,double t) { return lerp(start,end,ease("cubic",1,t)); }
    @Binding(value="ease_in_out_cubic",pure=true)
    public static double easeInOutCubic(double start,double end,double t) { return lerp(start,end,ease("cubic",2,t)); }
    @Binding(value="ease_in_elastic",pure=true)
    public static double easeInElastic(double start,double end,double t) { return lerp(start,end,ease("elastic",0,t)); }
    @Binding(value="ease_out_elastic",pure=true)
    public static double easeOutElastic(double start,double end,double t) { return lerp(start,end,ease("elastic",1,t)); }
    @Binding(value="ease_in_out_elastic",pure=true)
    public static double easeInOutElastic(double start,double end,double t) { return lerp(start,end,ease("elastic",2,t)); }
    @Binding(value="ease_in_expo",pure=true)
    public static double easeInExpo(double start,double end,double t) { return lerp(start,end,ease("expo",0,t)); }
    @Binding(value="ease_out_expo",pure=true)
    public static double easeOutExpo(double start,double end,double t) { return lerp(start,end,ease("expo",1,t)); }
    @Binding(value="ease_in_out_expo",pure=true)
    public static double easeInOutExpo(double start,double end,double t) { return lerp(start,end,ease("expo",2,t)); }
    @Binding(value="ease_in_quad",pure=true)
    public static double easeInQuad(double start,double end,double t) { return lerp(start,end,ease("quad",0,t)); }
    @Binding(value="ease_out_quad",pure=true)
    public static double easeOutQuad(double start,double end,double t) { return lerp(start,end,ease("quad",1,t)); }
    @Binding(value="ease_in_out_quad",pure=true)
    public static double easeInOutQuad(double start,double end,double t) { return lerp(start,end,ease("quad",2,t)); }
    @Binding(value="ease_in_quart",pure=true)
    public static double easeInQuart(double start,double end,double t) { return lerp(start,end,ease("quart",0,t)); }
    @Binding(value="ease_out_quart",pure=true)
    public static double easeOutQuart(double start,double end,double t) { return lerp(start,end,ease("quart",1,t)); }
    @Binding(value="ease_in_out_quart",pure=true)
    public static double easeInOutQuart(double start,double end,double t) { return lerp(start,end,ease("quart",2,t)); }
    @Binding(value="ease_in_quint",pure=true)
    public static double easeInQuint(double start,double end,double t) { return lerp(start,end,ease("quint",0,t)); }
    @Binding(value="ease_out_quint",pure=true)
    public static double easeOutQuint(double start,double end,double t) { return lerp(start,end,ease("quint",1,t)); }
    @Binding(value="ease_in_out_quint",pure=true)
    public static double easeInOutQuint(double start,double end,double t) { return lerp(start,end,ease("quint",2,t)); }
    @Binding(value="ease_in_sine",pure=true)
    public static double easeInSine(double start,double end,double t) { return lerp(start,end,ease("sine",0,t)); }
    @Binding(value="ease_out_sine",pure=true)
    public static double easeOutSine(double start,double end,double t) { return lerp(start,end,ease("sine",1,t)); }
    @Binding(value="ease_in_out_sine",pure=true)
    public static double easeInOutSine(double start,double end,double t) { return lerp(start,end,ease("sine",2,t)); }

    private void register(String name, DoubleFunction1 fn) {
        entries.put(name,ObjectProperty.property(new team.unnamed.mocha.runtime.value.NumericFunction() {
            public int arity() { return 1; }
            public double invoke(double a,double b,double c) { return fn.apply(a); }
        },true));
    }
    private void register(String name, DoubleFunction2 fn) {
        entries.put(name,ObjectProperty.property(new team.unnamed.mocha.runtime.value.NumericFunction() {
            public int arity() { return 2; }
            public double invoke(double a,double b,double c) { return fn.apply(a,b); }
        },true));
    }
    private void register(String name, DoubleFunction3 fn) {
        entries.put(name,ObjectProperty.property(new team.unnamed.mocha.runtime.value.NumericFunction() {
            public int arity() { return 3; }
            public double invoke(double a,double b,double c) { return fn.apply(a,b,c); }
        },true));
    }
    private static double bounceOut(double t) {
        if(t<1/2.75)return 7.5625*t*t;
        if(t<2/2.75){t-=1.5/2.75;return 7.5625*t*t+.75;}
        if(t<2.5/2.75){t-=2.25/2.75;return 7.5625*t*t+.9375;}
        t-=2.625/2.75;return 7.5625*t*t+.984375;
    }
    private static double easeIn(String shape,double t) {
        switch(shape){
            case "quad":return t*t;
            case "cubic":return t*t*t;
            case "quart":return t*t*t*t;
            case "quint":return t*t*t*t*t;
            case "sine":return 1-Math.cos(t*Math.PI/2);
            case "circ":return 1-Math.sqrt(1-t*t);
            case "expo":return t==0?0:Math.pow(2,10*t-10);
            case "back":return 2.70158*t*t*t-1.70158*t*t;
            case "bounce":return 1-bounceOut(1-t);
            case "elastic":return t==0||t==1?t:-Math.pow(2,10*t-10)*Math.sin((10*t-10.75)*(2*Math.PI/3));
            default:throw new IllegalArgumentException(shape);
        }
    }
    private static double ease(String shape,int mode,double t) {
        t=clamp(t,0,1);
        if(mode==0)return easeIn(shape,t);
        if(mode==1)return 1-easeIn(shape,1-t);
        if(shape.equals("back")){
            double c=1.70158*1.525;
            return t<.5?Math.pow(2*t,2)*((c+1)*2*t-c)/2:(Math.pow(2*t-2,2)*((c+1)*(t*2-2)+c)+2)/2;
        }
        if(shape.equals("elastic")){
            if(t==0||t==1)return t;
            double c=2*Math.PI/4.5;
            return t<.5?-(Math.pow(2,20*t-10)*Math.sin((20*t-11.125)*c))/2:Math.pow(2,-20*t+10)*Math.sin((20*t-11.125)*c)/2+1;
        }
        return t<.5?easeIn(shape,2*t)/2:1-easeIn(shape,2-2*t)/2;
    }

    @Override
    public @Nullable ObjectProperty getProperty(final @NotNull String name) {
        return entries.get(name);
    }
}
