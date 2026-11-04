# mocha

[![Build Status](https://img.shields.io/github/actions/workflow/status/unnamed/mocha/build.yml?branch=main)](https://github.com/unnamed/mocha/actions/workflows/build.yml)
[![MIT License](https://img.shields.io/badge/license-MIT-blue)](license.txt)
[![Discord](https://img.shields.io/discord/683899335405994062)](https://discord.gg/xbba2fy)

`mocha` is a lightweight, fast and efficient Molang lexer, parser, interpreter and compiler
for Java 8+. Molang is a simple **expression-based** language designed for fast and **data-driven**
calculation of values at run-time.

Its focus is to enable low-level systems like animation to support flexible data-driven
behavior, while staying highly performant.

Pretty much everything in this language evaluates to a number; if something doesn't evaluate
to a number, you can use an operator to make it into one. You can basically just think of
Molang as one big math equation.

This library lets programmers easily evaluate Molang expressions and bind objects
or functions so that they can be called from the evaluated expressions.

Check the [documentation](https://unnamed.team/docs/molang) for installation, usage and
some more information for this project
## EaseCation JVM program runtime (3.0.3-ec.3)

`compileProgram(source)` returns an immutable, context-independent JVM bytecode program.
Compile during resource preparation and retain the program; evaluate against current bindings:

```java
MochaEngine<?> engine = MochaEngine.createStandard();
Program program = engine.compileProgram("math.sin(query.anim_time * 90)");
Value value = program.evaluateFresh(entity, engine.scope());
```

`Program` is in `team.unnamed.mocha.runtime.program`, `Value` in `runtime.value`.
`evaluateFresh` isolates temporary variables; low-level `evaluate` uses caller-owned temp bindings
and preserves the sampling API's last-result convention. `prepareEval` and typed `compile` now
use this same compiler. `eval` remains the explicit AST interpreter/reference, not an automatic
retry after partial compiled execution.

The global cache is classloader-local, bounded by entry count and estimated memory weight.
It holds no host scope or entity. Compiled code reads dynamic queries, this, random functions and
assignments at execution. JVM JIT owns native machine code; no cross-process machine-code file is
loaded from content packs. See [the official-language coverage and execution contract](docs/molang-coverage.md)
for supported syntax/math, budgets, host responsibilities, precision limits and validation.

BedrockMotion animation/controller/render expressions and BEParticle share the same library;
weapon-specific algorithms do not belong in this runtime. Use the explicit fork artifact version
instead of replacing another release's bytes under the same coordinate.
