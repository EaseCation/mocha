/*
 * This file is part of mocha, licensed under the MIT License.
 *
 * Copyright (c) 2021-2026 Unnamed Team
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
package team.unnamed.mocha.runtime.value;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredObjectBindingTest {
    @Test
    void materializesTypedDottedPaths() {
        final Map<String, Object> values = new LinkedHashMap<String, Object>();
        values.put("mod.trace.pitch", 27.5D);
        values.put("context.first_person", true);
        values.put("owner.identifier", "minecraft:player");

        final StructuredObjectBinding binding = StructuredObjectBinding.from(values);
        assertEquals(27.5D, binding.getPath("mod.trace.pitch").getAsNumber(), 0D);
        assertTrue(binding.getPath("context.first_person").getAsBoolean());
        assertEquals("minecraft:player", binding.getPath("owner.identifier").getAsString());

        binding.clearPath("context.first_person");
        assertFalse(binding.getPath("context.first_person").getAsBoolean());
    }
}
