import team.unnamed.mocha.MochaEngine;
import team.unnamed.mocha.parser.ast.Expression;
import team.unnamed.mocha.runtime.program.Program;
import team.unnamed.mocha.runtime.value.NumberValue;
import java.util.*;

/** Diagnostic CPU comparison, not a unit-test assertion or a game/FPS benchmark. */
public final class ProgramBenchmark {
    private static volatile double sink;
    private static final int ITERATIONS = 100000;
    public static void main(String[] args) throws Exception {
        System.out.println("JVM="+System.getProperty("java.runtime.version")+", arch="+System.getProperty("os.arch"));
        for(String source:new String[]{
                "(this*1.5+2)/(this+3)",
                "math.sin(this*90)*3 + math.cos(this*45)*2 + math.clamp(this,0,1)",
                "t.x=this;loop(8,{t.x=math.sin(t.x)+1;});return t.x;"}) {
            MochaEngine<?> engine=MochaEngine.createStandard();
            List<Expression> ast=engine.parse(source);
            long start=System.nanoTime();Program program=engine.compileProgram(source);long compilation=System.nanoTime()-start;
            double[] interpreted=new double[7],compiled=new double[7];
            for(int round=-4;round<7;round++)for(int order=0;order<2;order++) {
                boolean bytecode=((round+order)&1)==0;
                start=System.nanoTime();double total=0;
                for(int i=0;i<ITERATIONS;i++) {
                    engine.scope().set("this",NumberValue.of((i%100)*.01));
                    total+=bytecode?program.evaluateFresh(null,engine.scope()).getAsNumber():engine.eval(ast);
                }
                sink=total;
                if(round>=0)(bytecode?compiled:interpreted)[round]=(System.nanoTime()-start)/(double)ITERATIONS;
            }
            Arrays.sort(compiled);Arrays.sort(interpreted);
            System.out.printf(Locale.ROOT,"%s%ncompile_ms=%.3f bytecode_bytes=%d interpreter_ns=%.1f bytecode_ns=%.1f ratio=%.2fx%n",
                    source,compilation/1e6,program.bytecodeSize(),interpreted[3],compiled[3],interpreted[3]/compiled[3]);
        }
    }
}
