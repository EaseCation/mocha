package team.unnamed.mocha.runtime;

import org.junit.jupiter.api.Test;
import team.unnamed.mocha.MochaEngine;
import team.unnamed.mocha.runtime.value.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class InterpreterSemanticsTest {
    static void both(String expression, double expected) {
        assertEquals(expected, MochaEngine.createStandard().eval(expression), 1e-9);
        assertEquals(expected, MochaEngine.createStandard().prepareEval(expression).evaluate(), 1e-9);
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
        for(Object[] c:cases)assertEquals((Double)c[1],MochaEngine.createStandard().prepareEval((String)c[0]).evaluate(),1e-9,(String)c[0]);
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
        {
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
            assertEquals(11,engine.prepareEval(script).evaluate());
            live[0]=false;
            script="(other->v.public) ?? 17";
            assertEquals(17,engine.prepareEval(script).evaluate());
        }
    }
    @Test void aliasesReadonlyForeachAndEmptyArrays() throws Exception {
        {
            MochaEngine<?> e=MochaEngine.createStandard();MutableObjectBinding ctx=new MutableObjectBinding();ctx.set("x",NumberValue.of(3));
            e.scope().set("context",ctx);e.scope().set("array",ArrayValue.of(new Value[]{NumberValue.of(9)}));
            String script="for_each(c.x,array,{t.x=1;});return c.x;";
            assertEquals(3,e.prepareEval(script).evaluate());
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

    @Test void preparedEvaluationDoesNotCompileOrCaptureMutableValues() {
        MochaEngine<?> engine = MochaEngine.createStandard();
        engine.postCompile(bytes -> fail("Prepared evaluation must not generate bytecode"));
        MutableObjectBinding query = new MutableObjectBinding();
        engine.scope().set("query", query);
        MochaFunction expression = engine.prepareEval("t.x=(t.x??0)+1;return t.x+(q.age??9);");
        query.set("age", NumberValue.of(2));
        assertEquals(3, expression.evaluate());
        query.set("age", NumberValue.zero());
        assertEquals(1, expression.evaluate());
        query.set("age", null);
        assertEquals(10, expression.evaluate());
        assertThrows(ClassNotFoundException.class, () -> Class.forName("team.unnamed.mocha.runtime.program.ProgramCompiler"));
    }
}
