package team.unnamed.mocha.runtime.program;

import javassist.*;
import team.unnamed.mocha.parser.MolangParser;
import team.unnamed.mocha.parser.ast.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Bounded classloader-local cache of context-independent JVM programs. No host execution at compile time. */
public final class ProgramCompiler {
    public static final String SEMANTICS_VERSION = "molang-modern-1";
    private static final ProgramCompiler SHARED = new ProgramCompiler(256, 16 * 1024 * 1024);
    private static final AtomicLong SERIAL = new AtomicLong();
    private final int capacity, byteBudget;
    private final ClassPool classPool;
    private int bytes;
    private long retainedWeight;
    private long builds, hits;
    private final Map<String,Program> cache = new LinkedHashMap<String,Program>(16,.75f,true);
    public ProgramCompiler(int capacity,int byteBudget) {
        if (capacity < 1 || byteBudget < 1) throw new IllegalArgumentException("Positive cache limits required");
        this.capacity=capacity;this.byteBudget=byteBudget;
        classPool=new ClassPool(true);
        classPool.insertClassPath(new LoaderClassPath(CompiledRuntime.class.getClassLoader()));
        classPool.importPackage("team.unnamed.mocha.runtime.value");
        classPool.importPackage("team.unnamed.mocha.runtime");
        classPool.importPackage("team.unnamed.mocha.runtime.program");
        classPool.importPackage("team.unnamed.mocha.parser.ast");
    }
    public static ProgramCompiler shared() { return SHARED; }
    public Program compile(String source) throws IOException {
        Objects.requireNonNull(source,"source");
        if(source.length()>262144) throw new IllegalArgumentException("Molang source exceeds 256K character budget");
        try { return compile(MolangParser.parser(new StringReader(source)).parseAll()); }
        catch(StackOverflowError error) { throw new IllegalArgumentException("Molang parser nesting budget exceeded",error); }
    }
    public synchronized Program compile(List<Expression> source) {
        AstSnapshot snapshot=new AstSnapshot();List<Expression> detached=snapshot.copy(source);
        String key=SEMANTICS_VERSION+snapshot.key;
        Program cached=cache.get(key);if(cached!=null){hits++;return cached;}
        Program result=new Generator(detached,classPool).build();builds++;
        // Large programs remain usable by their owner, but cannot evict the whole shared cache.
        long weight = result.bytecodeSize() + key.length()*2L + result.nodeCount()*128L;
        if(weight<=byteBudget){
            while(!cache.isEmpty()&&(cache.size()>=capacity||retainedWeight+weight>byteBudget)){
                Iterator<Map.Entry<String,Program>> it=cache.entrySet().iterator();Map.Entry<String,Program> entry=it.next();Program old=entry.getValue();
                retainedWeight-=old.bytecodeSize()+entry.getKey().length()*2L+old.nodeCount()*128L;it.remove();bytes-=old.bytecodeSize();
            }
            cache.put(key,result);bytes+=result.bytecodeSize();retainedWeight+=weight;
        }
        return result;
    }
    public synchronized void clear() { cache.clear();bytes=0;retainedWeight=0; }
    public synchronized Map<String,Long> statistics() {
        Map<String,Long> result=new LinkedHashMap<>();result.put("entries",(long)cache.size());result.put("bytecode_bytes",(long)bytes);result.put("estimated_retained_bytes",retainedWeight);result.put("builds",builds);result.put("hits",hits);return Collections.unmodifiableMap(result);
    }
    private static final class Loader extends ClassLoader {
        Loader(){super(CompiledRuntime.class.getClassLoader());}
        Class<?> define(String name,byte[] bytes){return defineClass(name,bytes,0,bytes.length);}
    }
    private static final class Generator {
        private static final String V="Value";
        private static final String N="NumberValue";
        private static final String R="RuntimeSemantics";
        private static final String F="CompiledRuntime$Frame";
        private final List<Expression> roots;
        private final List<Expression> nodes=new ArrayList<>();
        private final IdentityHashMap<Expression,Integer> ids=new IdentityHashMap<>();
        private CtClass type;
        private final ClassPool pool;
        private int blocks;
        Generator(List<Expression> roots,ClassPool pool){this.pool=pool;this.roots=roots;for(Expression e:roots)index(e);}
        int index(Expression e){
            Integer old=ids.get(e);if(old!=null)return old;
            int id=nodes.size();nodes.add(e);ids.put(e,id);
            if(e instanceof BinaryExpression){index(((BinaryExpression)e).left());index(((BinaryExpression)e).right());}
            else if(e instanceof UnaryExpression)index(((UnaryExpression)e).expression());
            else if(e instanceof AccessExpression)index(((AccessExpression)e).object());
            else if(e instanceof ArrayAccessExpression){index(((ArrayAccessExpression)e).array());index(((ArrayAccessExpression)e).index());}
            else if(e instanceof TernaryConditionalExpression){TernaryConditionalExpression t=(TernaryConditionalExpression)e;index(t.condition());index(t.trueExpression());index(t.falseExpression());}
            else if(e instanceof ExecutionScopeExpression)for(Expression child:((ExecutionScopeExpression)e).expressions())index(child);
            else if(e instanceof CallExpression){index(((CallExpression)e).function());for(Expression child:((CallExpression)e).arguments())index(child);}
            return id;
        }
        String call(Expression e){return "n"+ids.get(e)+"(f)";}
        String number(Expression e){
            if(e instanceof DoubleExpression)return Double.toString(team.unnamed.mocha.runtime.value.NumberValue.normalize(((DoubleExpression)e).value()));
            if(e instanceof BinaryExpression){
                BinaryExpression b=(BinaryExpression)e;String op=b.op()==BinaryExpression.Op.ADD?"+":b.op()==BinaryExpression.Op.SUB?"-":b.op()==BinaryExpression.Op.MUL?"*":null;
                if(op!=null)return N+".normalize("+"("+number(b.left())+")"+op+"("+number(b.right())+")"+")";
                if(b.op()==BinaryExpression.Op.DIV)return N+".normalize("+R+".divide("+number(b.left())+","+number(b.right())+"))";
            }
            if(e instanceof UnaryExpression&&((UnaryExpression)e).op()==UnaryExpression.Op.ARITHMETICAL_NEGATION)return "(-"+number(((UnaryExpression)e).expression())+")";
            return call(e)+".getAsNumber()";
        }
        String bool(Expression e){return call(e)+".getAsBoolean()";}
        String name(String value,int id) {
            // Only lexer-safe ASCII identifiers become Java constants. Arbitrary AST names and
            // all string payloads stay data, never Javassist source/escape sequences.
            for(int i=0;i<value.length();i++) {
                char c=value.charAt(i);
                if(!((c>='a'&&c<='z')||(c>='A'&&c<='Z')||(c>='0'&&c<='9')||c=='_'))return "name("+id+")";
            }
            return "\""+value+"\"";
        }
        String target(Expression e){
            if(e instanceof AccessExpression){AccessExpression a=(AccessExpression)e;return R+".struct("+target(a.object())+","+name(a.property(),ids.get(a))+")";}
            return call(e);
        }
        String execute(Expression e,String frame){return (e instanceof ExecutionScopeExpression ? "b" : "n")+ids.get(e)+"("+frame+")";}
        void method(String signature,String body)throws CannotCompileException{type.addMethod(CtNewMethod.make(signature+" {"+body+"}",type));}
        String sequence(List<Expression> expressions)throws CannotCompileException{
            List<String> chunks=new ArrayList<>();
            for(int i=0;i<expressions.size();i+=64){
                String name="seq"+(blocks++);StringBuilder code=new StringBuilder(V+" v="+N+".zero();");
                for(Expression e:expressions.subList(i,Math.min(i+64,expressions.size())))code.append("v=").append(branch(e)).append(";if(f.stopped())return v;");
                method("private "+V+" "+name+"("+F+" f)",code+"return v;");chunks.add(name);
            }
            StringBuilder body=new StringBuilder(V+" v="+N+".zero();");
            for(String c:chunks)body.append("v=").append(c).append("(f);if(f.stopped())return v;");
            return body+"return v;";
        }
        Program build(){
            String name="team.unnamed.mocha.runtime.program.generated.Program"+SERIAL.incrementAndGet();type=pool.makeClass(name);
            type.getClassFile().setMajorVersion(52); // Java 8 bytecode; no persisted JVM-specific machine code.
            try{
                type.setSuperclass(pool.get(CompiledRuntime.class.getName()));
                for(int i=0;i<nodes.size();i++){
                    method("private "+V+" n"+i+"("+F+" f)","return null;");
                    if(nodes.get(i) instanceof ExecutionScopeExpression)method("private "+V+" b"+i+"("+F+" f)","return null;");
                }
                for(int i=0;i<nodes.size();i++){
                    Expression e=nodes.get(i);type.getDeclaredMethod("n"+i).setBody("{"+F+" f=$1;"+emit(e,i)+"}");
                    if(e instanceof ExecutionScopeExpression)type.getDeclaredMethod("b"+i).setBody("{"+F+" f=$1;"+sequence(((ExecutionScopeExpression)e).expressions()).replace("return v;", "return "+N+".zero();")+"}");
                }
                method("public "+V+" run("+F+" f)",sequence(roots));
                method("public boolean complex()","return "+(roots.size()>1)+";");
                for(boolean body:new boolean[]{false,true}){
                    StringBuilder dispatch=new StringBuilder();
                    for(int start=0;start<nodes.size();start+=128){
                        String group=(body?"bdispatch":"ndispatch")+start;StringBuilder chunk=new StringBuilder("switch(i){");
                        for(int i=start;i<Math.min(start+128,nodes.size());i++)if(!body||nodes.get(i) instanceof ExecutionScopeExpression)chunk.append("case ").append(i).append(":return ").append(body?"b":"n").append(i).append("(f);");
                        method("private "+V+" "+group+"(int i,"+F+" f)",chunk+"default:throw new IllegalArgumentException(\"Invalid node\");}");
                        dispatch.append("if(i<").append(start+128).append(")return ").append(group).append("(i,f);");
                    }
                    method("public "+V+" "+(body?"body":"node")+"(int i,"+F+" f)",dispatch+"throw new IllegalArgumentException(\"Invalid node\");");
                }
                byte[] bytes=type.toBytecode();if(bytes.length>2*1024*1024)throw new IllegalArgumentException("Molang bytecode exceeds 2MiB program budget");
                CompiledRuntime program=(CompiledRuntime)new Loader().define(name,bytes).getDeclaredConstructor().newInstance();
                int[][] arguments=new int[nodes.size()][];
                for(int i=0;i<nodes.size();i++)if(nodes.get(i) instanceof CallExpression){
                    List<Expression> args=((CallExpression)nodes.get(i)).arguments();arguments[i]=new int[args.size()];
                    for(int j=0;j<args.size();j++)arguments[i][j]=ids.get(args.get(j));
                }
                program.initialize(nodes.toArray(new Expression[0]),bytes,arguments);return program;
            }catch(CannotCompileException|NotFoundException|IOException|ReflectiveOperationException e){throw new IllegalArgumentException("Molang bytecode compilation failed",e);}
            finally{type.detach();}
        }
        String emit(Expression e,int id){
            if(e instanceof DoubleExpression || e instanceof StringExpression)return "return constant("+id+");";
            if(e instanceof IdentifierExpression)return "return "+R+".lookup(f.scope,"+name(((IdentifierExpression)e).name(),id)+");";
            if(e instanceof AccessExpression){AccessExpression a=(AccessExpression)e;return "return "+R+".property("+call(a.object())+","+name(a.property(),ids.get(a))+");";}
            if(e instanceof ArrayAccessExpression){ArrayAccessExpression a=(ArrayAccessExpression)e;return "return "+R+".array("+call(a.array())+","+call(a.index())+");";}
            if(e instanceof ExecutionScopeExpression)return "return block("+id+",f);";
            if(e instanceof StatementExpression)return "f.control=StatementExpression$Op."+((StatementExpression)e).op().name()+";return "+N+".zero();";
            if(e instanceof UnaryExpression){UnaryExpression u=(UnaryExpression)e;switch(u.op()){
                case RETURN:return "f.returned="+call(u.expression())+";return "+N+".zero();";
                case LOGICAL_NEGATION:return "return "+V+".of(!"+bool(u.expression())+");";
                case ARITHMETICAL_NEGATION:return "return "+N+".of(-"+number(u.expression())+");";
            }}
            if(e instanceof TernaryConditionalExpression){TernaryConditionalExpression t=(TernaryConditionalExpression)e;return "if("+bool(t.condition())+")return "+branch(t.trueExpression())+";return "+branch(t.falseExpression())+";";}
            if(e instanceof BinaryExpression)return binary((BinaryExpression)e);
            if(e instanceof CallExpression)return function((CallExpression)e,id);
            throw new IllegalArgumentException("Unsupported AST: "+e.getClass());
        }
        String branch(Expression e){return e instanceof ExecutionScopeExpression ? execute(e,"f") : call(e);}
        String binary(BinaryExpression b){
            Expression a=b.left(),c=b.right();String left="("+number(a)+")",right="("+number(c)+")";
            switch(b.op()){
                case ADD:return "return "+N+".of("+left+"+"+right+");";
                case SUB:return "return "+N+".of("+left+"-"+right+");";
                case MUL:return "return "+N+".of("+left+"*"+right+");";
                case DIV:return "return "+N+".of("+R+".divide("+left+","+right+"));";
                case LT:case LTE:case GT:case GTE:
                    String op=b.op()==BinaryExpression.Op.LT?"<":b.op()==BinaryExpression.Op.LTE?"<=":b.op()==BinaryExpression.Op.GT?">":">=";
                    return "return "+V+".of("+left+op+right+");";
                case AND:return "return "+V+".of("+bool(a)+"&&"+bool(c)+");";
                case OR:return "return "+V+".of("+bool(a)+"||"+bool(c)+");";
                case EQ:case NEQ:return "return "+V+".of("+(b.op()==BinaryExpression.Op.NEQ?"!":"")+R+".equal("+call(a)+","+call(c)+"));";
                case NULL_COALESCE:return V+" v="+call(a)+";return v.isNil()?"+call(c)+":v;";
                case ASSIGN:
                    return V+" v="+call(c)+";"+assign(a,"v","f",0)+"return v;";
                case CONDITIONAL:return "if("+bool(a)+")return executeBlock("+call(c)+",f);return "+N+".zero();";
                case ARROW:return F+" child=f.actorChild("+call(a)+");if(child==null)return "+V+".nil();"+V+" result="+execute(c,"child")+";if(child.returned!=null)f.returned=child.returned;return result;";

            }
            throw new IllegalArgumentException("Unknown binary operator");
        }
        String assign(Expression target,String value,String frame,int depth) {
            if(target instanceof BinaryExpression && ((BinaryExpression)target).op()==BinaryExpression.Op.ARROW){
                BinaryExpression a=(BinaryExpression)target;String child="actor"+depth;
                return F+" "+child+"="+frame+".actorChild(n"+ids.get(a.left())+"("+frame+"));if("+child+"!=null){"+assign(a.right(),value,child,depth+1)+"}";
            }
            if(!(target instanceof AccessExpression))return "";
            AccessExpression a=(AccessExpression)target;
            Expression root=a;while(root instanceof AccessExpression)root=((AccessExpression)root).object();
            if(root instanceof IdentifierExpression && ( ((IdentifierExpression)root).name().equals("context") || ((IdentifierExpression)root).name().equals("c")))return "";
            return R+".assign("+target(a.object()).replace("(f)","("+frame+")")+","+name(a.property(),ids.get(a))+","+value+");";
        }
        String function(CallExpression e,int id){
            List<Expression> args=e.arguments();String name=e.function() instanceof IdentifierExpression?((IdentifierExpression)e.function()).name():"";
            if(name.equals("loop")||name.equals("for_each")){
                int expected=name.equals("loop")?2:3;if(args.size()!=expected)throw new IllegalArgumentException(name+" requires "+expected+" arguments");
                Expression body=args.get(expected-1);String control="if(child.returned!=null){f.returned=child.returned;break;}if(child.control==StatementExpression$Op.BREAK)break;";
                if(name.equals("loop"))return "int count="+R+".loopCount("+number(args.get(0))+");for(int i=0;i<count;i++){"+F+" child=f.child(f.actor);child.iteration();"+execute(body,"child")+";"+control+"}return "+N+".zero();";
                if(!(args.get(0) instanceof AccessExpression))throw new IllegalArgumentException("for_each target must be a property");

                return V+" values="+call(args.get(1))+";if(values instanceof ArrayValue){"+V+"[] items=((ArrayValue)values).values();for(int i=0;i<items.length;i++){"+assign(args.get(0),"items[i]","f",0)+F+" child=f.child(f.actor);child.iteration();"+execute(body,"child")+";"+control+"}}return "+N+".zero();";
            }
            return "return call("+id+",f,"+call(e.function())+");";
        }
    }
}
