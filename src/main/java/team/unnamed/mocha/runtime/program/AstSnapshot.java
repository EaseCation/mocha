package team.unnamed.mocha.runtime.program;

import team.unnamed.mocha.parser.ast.*;
import java.util.*;

/** Detaches mutable parser nodes and gives cache keys unambiguous structural encoding. */
final class AstSnapshot implements ExpressionVisitor<Expression> {
    final StringBuilder key = new StringBuilder();
    int count, depth;
    Expression copy(Expression e) {
        if (++count > 12000 || ++depth > 256) throw new IllegalArgumentException("Molang program exceeds node/depth budget");
        try { return e.visit(this); } finally { depth--; }
    }
    List<Expression> copy(List<Expression> entries) {
        List<Expression> result = new ArrayList<>(); key.append('[').append(entries.size()).append(':');
        for (Expression e: entries) result.add(copy(e)); key.append(']'); return result;
    }
    void atom(String kind, String text) { if(text.length()>262144 || key.length()+text.length()>1048576)throw new IllegalArgumentException("Molang program text budget exceeded"); key.append(kind).append(text.length()).append(':').append(text); }
    public Expression visit(Expression e) { throw new IllegalArgumentException("Unknown Molang AST: " + e.getClass().getName()); }
    public Expression visitDouble(DoubleExpression e) { atom("D",Double.toHexString(e.value()));return new DoubleExpression(e.value()); }
    public Expression visitString(StringExpression e) { atom("S",e.value());return new StringExpression(e.value()); }
    public Expression visitIdentifier(IdentifierExpression e) { atom("I",e.name());return new IdentifierExpression(e.name()); }
    public Expression visitAccess(AccessExpression e) { atom("A",e.property());return new AccessExpression(copy(e.object()),e.property()); }
    public Expression visitArrayAccess(ArrayAccessExpression e) { key.append('X');return new ArrayAccessExpression(copy(e.array()),copy(e.index())); }
    public Expression visitBinary(BinaryExpression e) { atom("B",e.op().name());return new BinaryExpression(e.op(),copy(e.left()),copy(e.right())); }
    public Expression visitUnary(UnaryExpression e) { atom("U",e.op().name());return new UnaryExpression(e.op(),copy(e.expression())); }
    public Expression visitStatement(StatementExpression e) { atom("T",e.op().name());return new StatementExpression(e.op()); }
    public Expression visitTernaryConditional(TernaryConditionalExpression e) { key.append('?');return new TernaryConditionalExpression(copy(e.condition()),copy(e.trueExpression()),copy(e.falseExpression())); }
    public Expression visitExecutionScope(ExecutionScopeExpression e) { key.append('{');return new ExecutionScopeExpression(copy(e.expressions())); }
    public Expression visitCall(CallExpression e) { key.append('C');return new CallExpression(copy(e.function()),copy(e.arguments())); }
}
