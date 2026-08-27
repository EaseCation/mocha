package team.unnamed.mocha.runtime;

import org.junit.jupiter.api.Test;
import team.unnamed.mocha.MochaEngine;
import team.unnamed.mocha.runtime.value.Function;
import team.unnamed.mocha.runtime.value.MutableObjectBinding;
import team.unnamed.mocha.runtime.value.Value;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringEqualityTest {
    @Test
    void stringEqualityDoesNotCollapseToNumericZero() {
        final MochaEngine<?> engine = MochaEngine.createStandard();

        assertEquals(1.0D, engine.eval("'crossbow' == 'crossbow'"));
        assertEquals(0.0D, engine.eval("'crossbow' == 'bow'"));
        assertEquals(1.0D, engine.eval("'crossbow' != 'bow'"));
    }

    @Test
    void callableStringValueSupportsPropertyAndFunctionQueryForms() {
        final MochaEngine<?> engine = MochaEngine.createStandard();
        final MutableObjectBinding query = new MutableObjectBinding();
        query.set("get_equipped_item_name", new CallableString("crossbow", "shield"));
        engine.scope().set("query", query);

        assertEquals(1.0D, engine.eval("query.get_equipped_item_name == 'crossbow'"));
        assertEquals(1.0D, engine.eval("query.get_equipped_item_name('off_hand') == 'shield'"));
        assertEquals(1.0D, engine.eval("query.get_equipped_item_name != 'bow'"));
    }

    private record CallableString(String defaultValue, String argumentValue) implements Function<Object> {
        @Override
        public Value evaluate(ExecutionContext<Object> context, Arguments arguments) {
            return Value.of(arguments.length() == 0 ? defaultValue : argumentValue);
        }

        @Override
        public boolean isString() {
            return true;
        }

        @Override
        public String getAsString() {
            return defaultValue;
        }
    }
}
