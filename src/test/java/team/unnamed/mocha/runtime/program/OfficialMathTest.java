package team.unnamed.mocha.runtime.program;

import org.junit.jupiter.api.*;
import team.unnamed.mocha.MochaEngine;
import team.unnamed.mocha.runtime.standard.MochaMath;
import team.unnamed.mocha.runtime.value.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** Expectations from MicrosoftDocs, not generated from a second interpreter. */
class OfficialMathTest {
    static final Object[][] CASES={
        {"math.abs(-4)",4d},{"math.acos(0)",90d},{"math.asin(1)",90d},{"math.atan(1)",45d},
        {"math.atan2(1,-1)",135d},{"math.ceil(-1.2)",-1d},{"math.clamp(5,1,3)",3d},
        {"math.copy_sign(3,-2)",-3d},{"math.cos(180)",-1d},{"math.exp(0)",1d},
        {"math.floor(-1.2)",-2d},{"math.hermite_blend(0.5)",.5d},
        {"math.inverse_lerp(10,20,15)",.5d},{"math.inverse_lerp(2,2,7)",0d},
        {"math.lerp(20,10,0.25)",17.5d},{"math.lerprotate(50,-10,0.25)",35d},
        {"math.lerprotate(350,10,0.5)",360d},{"math.lerprotate(10,350,0.5)",0d},
        {"math.ln(1)",0d},{"math.max(-3,4)",4d},{"math.min(-3,4)",-3d},
        {"math.min_angle(180)",-180d},{"math.min_angle(-181)",179d},{"math.min_angle(720)",0d},
        {"math.mod(7,3)",1d},{"math.mod(-7,3)",-1d},{"math.mod(7,0)",0d},{"math.pi",Math.PI},
        {"math.pow(2,3)",8d},{"math.round(2.2)",2d},{"math.sign(0)",-1d},
        {"math.sign(3)",1d},{"math.sign(-3)",-1d},{"math.sin(90)",1d},{"math.sqrt(9)",3d},
        {"math.trunc(-1.8)",-1d},{"math.random(4,4)",4d},{"math.random_integer(4,4)",4d},
        {"math.die_roll(3,2,2)",6d},{"math.die_roll_integer(3,2,2)",6d}
    };
    @Test void officialInventoryHasNoMissingBindingsOrUntestedNames() throws Exception {
        Set<String> covered=new HashSet<>();
        for(Object[] entry:CASES)covered.add(((String)entry[0]).substring(5).split("\\(")[0]);
        for(String shape:Arrays.asList("back","bounce","circ","cubic","elastic","expo","quad","quart","quint","sine"))
            for(String mode:Arrays.asList("in","out","in_out"))covered.add("ease_"+mode+"_"+shape);
        Set<String> documented=new HashSet<>();
        try(java.io.BufferedReader reader=new java.io.BufferedReader(new java.io.InputStreamReader(
                getClass().getResourceAsStream("/official-math-functions.txt"),java.nio.charset.StandardCharsets.UTF_8))) {
            for(String name;(name=reader.readLine())!=null;)if(!name.isEmpty()&&!name.startsWith("#"))documented.add(name);
        }
        assertEquals(documented,covered);
        ObjectValue math=(ObjectValue)MochaEngine.createStandard().scope().get("math");
        for(String name:documented)assertFalse(math.get(name).isNil(),name);
    }
    @TestFactory Stream<DynamicTest> documentedMath() {
        return Arrays.stream(CASES).map(c->DynamicTest.dynamicTest((String)c[0],()->{
            String source=(String)c[0];double expected=(Double)c[1];
            MochaEngine<?> engine=MochaEngine.createStandard();
            assertEquals(expected,engine.eval(source),1e-9,"interpreted");
            assertEquals(expected,engine.compileProgram(source).evaluateFresh(null,engine.scope()).getAsNumber(),1e-9,"bytecode");
            engine.scope().set("math",new MochaMath());
            assertEquals(expected,engine.compileProgram(source).evaluateFresh(null,engine.scope()).getAsNumber(),1e-9,"direct math binding");
        }));
    }
    @TestFactory Stream<DynamicTest> allDocumentedEasingNamesAndEndpoints() {
        List<DynamicTest> tests=new ArrayList<>();
        for(String shape:Arrays.asList("back","bounce","circ","cubic","elastic","expo","quad","quart","quint","sine"))for(String mode:Arrays.asList("in","out","in_out")){
            String name="math.ease_"+mode+"_"+shape;
            tests.add(DynamicTest.dynamicTest(name,()->{
                MochaEngine<?> engine=MochaEngine.createStandard();
                for(double t:new double[]{0,.1,.25,.5,.75,.9,1}){
                    String source=name+"(10,20,"+t+")";
                    double interpreted=engine.eval(source),compiled=engine.compileProgram(source).evaluateFresh(null,engine.scope()).getAsNumber();
                    assertTrue(Double.isFinite(compiled));assertEquals(interpreted,compiled,1e-9);
                    if(t==0)assertEquals(10,compiled,1e-9);if(t==1)assertEquals(20,compiled,1e-9);
                    if(mode.equals("in_out")&&t==.5)assertEquals(15,compiled,1e-9);
                }
            }));
        }
        return tests.stream();
    }
    @Test void randomFunctionsRemainDynamicAndInclusive() throws Exception {
        MochaEngine<?> engine=MochaEngine.createStandard();Program program=engine.compileProgram("math.random_integer(0,1)");Set<Double> values=new HashSet<>();
        for(int i=0;i<256;i++)values.add(program.evaluateFresh(null,engine.scope()).getAsNumber());
        assertEquals(new HashSet<>(Arrays.asList(0d,1d)),values);
        for(String source:Arrays.asList("math.random(-2,3)","math.die_roll(4,-2,3)","math.die_roll_integer(4,-2,3)")){
            Program p=engine.compileProgram(source);double low=source.contains("die_roll")?-8:-2,high=source.contains("die_roll")?12:3;
            for(int i=0;i<128;i++){double value=p.evaluateFresh(null,engine.scope()).getAsNumber();assertTrue(value>=low&&value<=high,source);}
        }
    }
}
