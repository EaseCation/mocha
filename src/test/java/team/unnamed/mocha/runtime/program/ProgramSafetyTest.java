package team.unnamed.mocha.runtime.program;

import org.junit.jupiter.api.Test;
import team.unnamed.mocha.MochaEngine;
import team.unnamed.mocha.runtime.*;
import team.unnamed.mocha.runtime.compiled.*;
import team.unnamed.mocha.runtime.value.*;
import team.unnamed.mocha.runtime.binding.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ProgramSafetyTest {
    static void both(String expression, double expected) throws Exception {
        MochaEngine<?> a=MochaEngine.createStandard(),b=MochaEngine.createStandard();
        assertEquals(expected,a.eval(expression),1e-9,"interpreter: "+expression);
        assertEquals(expected,b.compileProgram(expression).evaluateFresh(null,b.scope()).getAsNumber(),1e-9,"bytecode: "+expression);
    }
    @Test void standaloneBlocksSimpleLoopsAndSharedStructs() throws Exception {
        both("{v.x=5;return v.x+2;}",7);
        both("{7;}",0);
        both(".5 + 1e-2 + 2.5E+1f",25.51);
        both("v.x=2;return v.x - -0.25;",2.25);
        both("v.x=2;return (v.x - -0.25) * (v.x - -0.25);",5.0625);
        both("0.00000000000000006123233995736766 * 2",1.2246467991473532E-16);
        both("v.x=0;loop(3,v.x=v.x+1);return v.x;",3);
        both("v.x=1;v.y=2;",0);
        both("v.a=v.b=5;return v.a+v.b;",10);
        both("v.a.x=3;v.b=v.a;v.b.x=9;return v.a.x;",9);
        both("return true ? false ? 1 : 2 : 3;",2);
        both("return v.missing ?? v.other ?? 7;",7);
        both("{loop(4,{continue;});return 8;}",8);
    }
    @Test void actorPermissionsStalenessAndContextAreHostOwned() throws Exception {
        for(boolean compiled:new boolean[]{false,true}) {
            MochaEngine<?> engine=MochaEngine.createStandard();
            Scope remote=Scope.create();MutableObjectBinding fields=new MutableObjectBinding();
            fields.set("public",NumberValue.of(7));fields.block();remote.set("variable",fields);
            final boolean[] live={true};
            engine.scope().set("other",new ActorReference() {
                public Object entity(){return this;}
                public Scope bindings(){return remote;}
                public boolean valid(){return live[0];}
            });
            MutableObjectBinding context=new MutableObjectBinding();context.set("value",NumberValue.of(4));engine.scope().set("context",context);
            String script="other->v.public=9;c.value=12;return (other->v.public)+c.value;";
            assertEquals(11,compiled?engine.compileProgram(script).evaluateFresh(null,engine.scope()).getAsNumber():engine.eval(script));
            live[0]=false;
            script="(other->v.public) ?? 17";
            assertEquals(17,compiled?engine.compileProgram(script).evaluateFresh(null,engine.scope()).getAsNumber():engine.eval(script));
        }
    }
    @Test void aliasesReadonlyForeachAndEmptyArrays() throws Exception {
        for(boolean compiled:new boolean[]{false,true}) {
            MochaEngine<?> e=MochaEngine.createStandard();MutableObjectBinding ctx=new MutableObjectBinding();ctx.set("x",NumberValue.of(3));
            e.scope().set("context",ctx);e.scope().set("array",ArrayValue.of(new Value[]{NumberValue.of(9)}));
            String script="for_each(c.x,array,{t.x=1;});return c.x;";
            assertEquals(3,compiled?e.compileProgram(script).evaluateFresh(null,e.scope()).getAsNumber():e.eval(script));
        }
    }
    @Test void stringConstantsAreDataAndLocaleIndependent() throws Exception {
        Locale previous=Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            both("V.INDEX=3;RETURN v.index;",3);
            both("'混合\\text\"; return 99;' == '混合\\text\"; return 99;'",1);
            both("'A' == 'a'",0);
        } finally {Locale.setDefault(previous);}
    }
    @Test void stringPayloadAndFunctionReferencesAreNotReinterpreted() throws Exception {
        String payload="混合\\text\";return 99;";
        Program p=ProgramCompiler.shared().compile("'"+payload+"'");
        assertEquals(payload,p.evaluate(null,Scope.create()).getAsString());
        Scope scope=Scope.create();int[] calls={0};
        Function<Object> function=(c,a)->{calls[0]++;return NumberValue.of(3);};
        scope.set("function",function);scope.set("other",new JavaValue(7));
        assertSame(function,ProgramCompiler.shared().compile("other->function").evaluate(null,scope));
        assertEquals(0,calls[0]);
    }
    @Test void nullCallbacksAndThrowingCallbacksAreNeverReplayed() throws Exception {
        MochaEngine<?> e=MochaEngine.createStandard();int[] invoked={0};
        e.scope().set("absent",(Function<Object>)(c,a)->null);
        both("v.unknown ?? 5",5);
        assertEquals(5,e.compileProgram("absent() ?? 5").evaluate(null,e.scope()).getAsNumber());
        assertEquals(5,e.eval("absent() ?? 5"));
        e.scope().set("fail",(Function<Object>)(c,a)->{invoked[0]++;throw new IllegalStateException("host");});
        Program code=e.compileProgram("fail()");
        assertThrows(IllegalStateException.class,()->code.evaluate(null,e.scope()));assertEquals(1,invoked[0]);
        e.scope().set("fail",(Function<Object>)(c,a)->NumberValue.of(8));
        assertEquals(8,code.evaluate(null,e.scope()).getAsNumber());
    }
    @Test void concurrentEvaluationsDoNotShareTempOrActors() throws Exception {
        Program p=ProgramCompiler.shared().compile("t.x=who();loop(8,{t.x=t.x+1;});return t.x;");
        ExecutorService threads=Executors.newFixedThreadPool(4);
        try {
            List<Future<Double>> results=new ArrayList<>();
            for(int i=0;i<32;i++){final int actor=i;results.add(threads.submit(()->{
                Scope scope=MochaEngine.createStandard().scope();scope.set("who",(Function<Object>)(c,a)->Value.of((Number)c.entity()));
                return p.evaluateFresh(actor,scope).getAsNumber();
            }));}
            for(int i=0;i<32;i++)assertEquals(i+8,results.get(i).get());
        } finally { threads.shutdownNow(); }
    }
    @Test void nestedLoopsAreBoundedInBothBackends() throws Exception {
        String source="loop(1024,{loop(1024,{loop(2,{v.x=1;});});});";
        MochaEngine<?> engine=MochaEngine.createStandard();
        assertThrows(IllegalStateException.class,()->engine.eval(source));
        Program program=engine.compileProgram(source);
        assertThrows(IllegalStateException.class,()->program.evaluateFresh(null,engine.scope()));
    }
    @Test void failedCompilationDoesNotPoisonCacheAndBytesAreDetached() throws Exception {
        ProgramCompiler compiler=new ProgramCompiler(2,100000);
        assertThrows(IllegalArgumentException.class,()->compiler.compile("for_each(1,2,3)"));
        Program p=compiler.compile("12+3");byte[] bytes=p.bytecode();bytes[0]=0;
        assertEquals(0xca,p.bytecode()[0]&255);assertEquals(52,p.bytecode()[7]&255);assertEquals(15,p.evaluate(null,Scope.create()).getAsNumber());
        assertTrue(compiler.statistics().get("estimated_retained_bytes")<=100000);
    }
    public interface Sum extends MochaCompiledFunction {
        double eval(@Named("count") double count);
        default double twice(double count) { return eval(count)*2; }
    }
    @Test void publicPreparedAndTypedEntriesUseFullControlFlow() {
        MochaEngine<?> engine=MochaEngine.createStandard();
        assertEquals(6,engine.prepareEval("t.x=0;loop(3,{t.x=t.x+2;});return t.x;").evaluate());
        Sum sum=engine.compile("t.x=0;loop(count,{t.x=t.x+2;});return t.x;",Sum.class);
        assertEquals(8,sum.eval(4));assertEquals(6,sum.eval(3));assertEquals(12,sum.twice(3));
    }
    public static class NumericHost {
        int calls;
        @Binding("add") public double add(double a,double b) {calls++;return a+b;}
        @Binding("explode") public double explode(double x) {calls++;throw new IllegalStateException("host failure");}
    }
    @Test void numericBridgeUsesCurrentHostAndRespectsEvaluationOrder() throws Exception {
        NumericHost host=new NumericHost();MochaEngine<?> e=MochaEngine.createStandard();e.bindInstance(NumericHost.class,host,"host");
        Program code=e.compileProgram("v.x=1;return host.add(v.x=v.x+1,v.x=v.x+1);");
        assertEquals(5,code.evaluateFresh(null,e.scope()).getAsNumber());assertEquals(1,host.calls);
        assertEquals(2,e.compileProgram("host.add(2)").evaluate(null,e.scope()).getAsNumber());
        assertThrows(RuntimeException.class,()->e.compileProgram("host.explode(1)").evaluate(null,e.scope()));
        assertEquals(3,host.calls);
    }
}
