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
package team.unnamed.mocha.runtime;

import javassist.ClassPool;
import team.unnamed.mocha.parser.ast.Expression;
import team.unnamed.mocha.runtime.compiled.*;
import team.unnamed.mocha.runtime.program.*;
import team.unnamed.mocha.runtime.value.*;
import java.lang.reflect.*;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.function.Consumer;

/** Typed host adapter over the single context-independent bytecode compiler. */
public final class MolangCompiler {
    private final Object entity;
    private final Scope scope;
    private Consumer<byte[]> postCompile;
    public MolangCompiler(Object entity,ClassLoader loader,Scope scope) {
        this.entity=entity;this.scope=Objects.requireNonNull(scope);Objects.requireNonNull(loader);
    }
    public Object entity(){return entity;}
    /** Kept as an inspection API; expression bytecode never resolves arbitrary host class names. */
    public ClassPool classPool(){return ClassPool.getDefault();}
    public void postCompile(Consumer<byte[]> callback){postCompile=callback;}
    public <T extends MochaCompiledFunction> T compile(List<Expression> expressions,Class<T> type){
        if(!type.isInterface())throw new IllegalArgumentException("Target must be an interface");
        Method selected=null;
        for(Method method:type.getMethods())if(Modifier.isAbstract(method.getModifiers())&&!Modifier.isStatic(method.getModifiers())){
            if(isObjectMethod(method))continue;
            if(selected!=null&&!sameSignature(selected,method))throw new IllegalArgumentException("Target must have one abstract method");
            if(selected==null||selected.getReturnType().isAssignableFrom(method.getReturnType()))selected=method;
        }
        if(selected==null)throw new IllegalArgumentException("Target has no abstract method");
        final Method target=selected;final String[] names=new String[target.getParameterCount()];
        Set<String> unique=new HashSet<>();
        for(int i=0;i<names.length;i++){
            Parameter p=target.getParameters()[i];Named named=p.getAnnotation(Named.class);
            if(named==null&&!p.isNamePresent())throw new IllegalArgumentException("Parameter requires @Named: "+p);
            names[i]=(named==null?p.getName():named.value()).toLowerCase(Locale.ROOT);
            if(!unique.add(names[i]))throw new IllegalArgumentException("Duplicate argument: "+names[i]);
        }
        Class<?> returns=target.getReturnType();
        if(!(returns==void.class||returns==double.class||returns==float.class||returns==long.class||returns==int.class||returns==short.class||returns==byte.class||returns==boolean.class||returns==String.class||returns==Value.class))throw new IllegalArgumentException("Unsupported return type: "+returns);
        final Program program=ProgramCompiler.shared().compile(expressions);
        if(postCompile!=null)postCompile.accept(program.bytecode());
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(proxy,method,args)->{
            if(isObjectMethod(method)){
                if(method.getName().equals("toString"))return "MolangBytecode("+program.nodeCount()+" nodes)";
                if(method.getName().equals("hashCode"))return System.identityHashCode(proxy);
                if(method.getName().equals("equals"))return proxy==args[0];
            }
            if(method.isDefault())return invokeDefault(proxy,method,args);
            if(!sameSignature(method,target))throw new UnsupportedOperationException("Not the compiled entry: "+method);
            Scope current=scope;
            if(names.length>0){current=scope.copy();for(int i=0;i<names.length;i++)current.set(names[i],Value.of(args[i]));}
            Value value=program.evaluateFresh(entity,current);
            if(returns==void.class)return null;
            if(returns==Value.class)return value;
            if(returns==String.class)return value.getAsString();
            if(returns==boolean.class)return value.getAsBoolean();
            double n=value.getAsNumber();
            if(returns==double.class)return n;
            if(returns==float.class)return (float)n;
            if(returns==long.class)return (long)n;
            if(returns==int.class)return (int)n;
            if(returns==short.class)return (short)n;
            return (byte)n;
        }));
    }
    private static boolean sameSignature(Method a,Method b) {
        return a.getName().equals(b.getName())&&Arrays.equals(a.getParameterTypes(),b.getParameterTypes());
    }
    private static boolean isObjectMethod(Method method) {
        try {Object.class.getMethod(method.getName(),method.getParameterTypes());return true;}
        catch(NoSuchMethodException ignored){return false;}
    }
    private static Object invokeDefault(Object proxy,Method method,Object[] args) throws Throwable {
        Class<?> owner=method.getDeclaringClass();MethodHandles.Lookup lookup;
        try {
            Method privateLookup=MethodHandles.class.getMethod("privateLookupIn",Class.class,MethodHandles.Lookup.class);
            lookup=(MethodHandles.Lookup)privateLookup.invoke(null,owner,MethodHandles.lookup());
        } catch(NoSuchMethodException java8) {
            Constructor<MethodHandles.Lookup> constructor=MethodHandles.Lookup.class.getDeclaredConstructor(Class.class,int.class);
            constructor.setAccessible(true);lookup=constructor.newInstance(owner,15);
        }
        return lookup.unreflectSpecial(method,owner).bindTo(proxy).invokeWithArguments(args==null?new Object[0]:args);
    }

}
