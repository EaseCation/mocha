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

import java.util.HashMap;
import java.util.Map;

/**
 * Mutable object binding that materializes dotted paths as real nested MoLang objects.
 * A key such as {@code mod.ecgun.fire} becomes {@code mod -> ecgun -> fire}.
 */
public class StructuredObjectBinding extends MutableObjectBinding {
    private final Map<String, StructuredObjectBinding> children = new HashMap<String, StructuredObjectBinding>();

    public void setPath(final String path, final Value value) {
        if (path == null || path.trim().isEmpty()) return;
        final String[] parts = path.split("\\.");
        StructuredObjectBinding current = this;
        for (int i = 0; i < parts.length - 1; i++) {
            final String part = parts[i];
            StructuredObjectBinding child = current.children.get(part);
            if (child == null) {
                child = new StructuredObjectBinding();
                current.children.put(part, child);
                current.set(part, child);
            }
            current = child;
        }
        current.set(parts[parts.length - 1], value);
    }

    public void setPath(final String path, final double value) {
        setPath(path, NumberValue.of(value));
    }

    public void setPath(final String path, final boolean value) {
        setPath(path, Value.of(value));
    }

    public void setPath(final String path, final String value) {
        setPath(path, Value.of(value));
    }

    public Value getPath(final String path) {
        if (path == null || path.trim().isEmpty()) return null;
        final String[] parts = path.split("\\.");
        StructuredObjectBinding current = this;
        for (int i = 0; i < parts.length - 1; i++) {
            current = current.children.get(parts[i]);
            if (current == null) return null;
        }
        return current.get(parts[parts.length - 1]);
    }

    public void clearPath(final String path) {
        setPath(path, Value.nil());
    }

    public static StructuredObjectBinding from(final Map<String, ?> values) {
        final StructuredObjectBinding binding = new StructuredObjectBinding();
        if (values == null) return binding;
        for (final Map.Entry<String, ?> entry : values.entrySet()) {
            final Object value = entry.getValue();
            if (value instanceof Boolean) binding.setPath(entry.getKey(), ((Boolean) value).booleanValue());
            else if (value instanceof Number) binding.setPath(entry.getKey(), ((Number) value).doubleValue());
            else if (value != null) binding.setPath(entry.getKey(), String.valueOf(value));
        }
        return binding;
    }
}
