/*
 * This file is part of mocha, licensed under the MIT license
 *
 * Copyright (c) 2021-2025 Unnamed Team
 */
package team.unnamed.mocha.runtime;

import org.junit.jupiter.api.Test;
import team.unnamed.mocha.MochaEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FloatSuffixTest {
    @Test
    void evaluatesBedrockFloatSuffixes() {
        final MochaEngine<?> engine = MochaEngine.createStandard();

        assertEquals(1.0D, engine.eval("(0.0f <= 0.0F) && 1f"));
    }
}
