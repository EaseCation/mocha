package team.unnamed.mocha.runtime.program;

import org.junit.jupiter.api.Test;
import team.unnamed.mocha.MochaEngine;
import team.unnamed.mocha.runtime.*;
import team.unnamed.mocha.runtime.value.*;
import team.unnamed.mocha.parser.ast.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ProgramCompilerTest {
    static double compiled(String source) throws Exception {
        return ProgramCompiler.shared().compile(source).evaluateFresh(null,MochaEngine.createStandard().scope()).getAsNumber();
    }
    @Test void operatorsAndOfficialControlFlow() throws Exception {
        Object[][] cases={
            {"1+2*3",7d},{"-2 * (3 + 4)",-14d},{"!false && true || false",1d},
            {"1 < 2 && 2 <= 2 && 4 > 3 && 4 >= 4",1d},{"1==2 || 2!=3",1d},
            {"'Case' == 'case'",0d},{"'Case' != 'case'",1d},{"6 / -3",-2d},{"1 / 0",0d},
            {"v.zero=0;return v.zero ?? 7;",0d},{"v.missing ?? 7",7d},
            {"v.x=1;v.y=1;loop(10,{t.x=v.x+v.y;v.x=v.y;v.y=t.x;});return v.y;",144d},
            {"v.x=0;loop(10,{loop(10,{v.x=v.x+1;(v.x>5)?break;});});return v.x;",15d},
            {"v.x=0;loop(10,{(v.x>5)?continue;v.x=v.x+1;});return v.x;",6d},
            {"loop(10,{loop(10,{return 17;});});return 99;",17d},
            {"v.x=0;false && (v.x=1);true || (v.x=2);return v.x;",0d},
            {"false ? {return 1;} : {return 2;};return 99;",2d},
            {"v.x=0;1 ? {v.x=7;};return v.x;",7d},
            {"true ? 2 : false ? 3 : 4",2d},{"false ? 2 : true ? 3 : 4",3d},
            {"math.sin(90)+math.cos(0)",2d},{"v.x=0;loop(3000,{v.x=v.x+1;});return v.x;",1024d},
            {"v.location.x=1;v.location.y=2;v.location.z=3;return v.location.x+v.location.y+v.location.z;",6d}
        };
        for(Object[] c:cases)assertEquals((Double)c[1],compiled((String)c[0]),1e-9,(String)c[0]);
    }
    @Test void dynamicContextArraysCallbacksAndThis() throws Exception {
        Scope scope=MochaEngine.createStandard().scope();scope.set("this",NumberValue.of(5));
        scope.set("items",ArrayValue.of(new Value[]{NumberValue.of(3),NumberValue.of(8)}));
        scope.set("empty",ArrayValue.of(new Value[0]));
        assertEquals(3,ProgramCompiler.shared().compile("items[-4]").evaluate(null,scope).getAsNumber());
        assertEquals(8,ProgramCompiler.shared().compile("items[3.9]").evaluate(null,scope).getAsNumber());
        assertEquals(12,ProgramCompiler.shared().compile("empty[1] ?? 12").evaluate(null,scope).getAsNumber());
        assertEquals(11,ProgramCompiler.shared().compile("v.sum=0;for_each(t.item,items,{v.sum=v.sum+t.item;});return v.sum;").evaluateFresh(null,scope).getAsNumber());
        scope.set("who",(Function<Object>)(ctx,args)->NumberValue.of(((Number)ctx.entity()).doubleValue()));
        scope.set("other",new JavaValue(23));
        assertEquals(28,ProgramCompiler.shared().compile("other->who() + this").evaluate(7,scope).getAsNumber());
        scope.set("other",new JavaValue(null));
        assertEquals(9,ProgramCompiler.shared().compile("(other->who()) ?? 9").evaluate(7,scope).getAsNumber());
        scope.set("choose",(Function<Object>)(ctx,args)->args.next().eval());
        assertEquals(5,ProgramCompiler.shared().compile("choose(5, v.bad=99)").evaluate(null,scope).getAsNumber());
        assertTrue(((ObjectValue)scope.get("v")).get("bad").isNil());
        scope.set("again",(Function<Object>)(ctx,args)->ctx.eval(args.next().expression()));
        assertEquals(6,ProgramCompiler.shared().compile("again(this+1)").evaluate(null,scope).getAsNumber());
    }
    @Test void cacheIsolationMutationEvictionAndConcurrency() throws Exception {
        ProgramCompiler compiler=new ProgramCompiler(2,1000000);
        Program first=compiler.compile("v.x=(v.x??0)+1;return v.x;");
        assertSame(first,compiler.compile("v.x = (v.x ?? 0) + 1; return v.x;"));
        Scope a=MochaEngine.createStandard().scope(),b=MochaEngine.createStandard().scope();
        assertEquals(1,first.evaluateFresh(null,a).getAsNumber());assertEquals(2,first.evaluateFresh(null,a).getAsNumber());assertEquals(1,first.evaluateFresh(null,b).getAsNumber());
        Program temp=compiler.compile("t.x=(t.x??0)+1;return t.x;");assertEquals(1,temp.evaluateFresh(null,a).getAsNumber());assertEquals(1,temp.evaluateFresh(null,a).getAsNumber());
        List<Expression> source=MochaEngine.createStandard().parse("7+9");Program frozen=compiler.compile(source);((BinaryExpression)source.get(0)).right(new DoubleExpression(100));assertEquals(16,frozen.evaluate(null,a).getAsNumber());
        assertEquals(2,compiler.statistics().get("entries"));compiler.clear();assertEquals(0,compiler.statistics().get("entries"));assertEquals(3,first.evaluate(null,a).getAsNumber());
        ExecutorService executor=Executors.newFixedThreadPool(4);
        try{
            List<Future<Program>> results=new ArrayList<>();for(int i=0;i<12;i++)results.add(executor.submit(()->compiler.compile("2*3+4")));
            Program expected=results.get(0).get();for(Future<Program> f:results)assertSame(expected,f.get());
        }finally{executor.shutdownNow();}
    }
    @Test void longProgramAndRejectionNeverExecuteAtCompileTime() throws Exception {
        StringBuilder source=new StringBuilder("v.x=0;");for(int i=0;i<1000;i++)source.append("v.x=v.x+1;");source.append("return v.x;");
        assertEquals(1000,compiled(source.toString()));
        Scope scope=MochaEngine.createStandard().scope();int[] invoked={0};scope.set("touch",(Function<Object>)(c,a)->NumberValue.of(++invoked[0]));
        Program p=ProgramCompiler.shared().compile("touch()+touch()");assertEquals(0,invoked[0]);assertEquals(3,p.evaluate(null,scope).getAsNumber());assertEquals(7,p.evaluate(null,scope).getAsNumber());
        assertThrows(IllegalArgumentException.class,()->ProgramCompiler.shared().compile("loop(1)"));
    }
}
