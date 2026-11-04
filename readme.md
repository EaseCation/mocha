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
## EaseCation interpreter runtime

Version `3.0.3-ec.4` retains the parser, interpreter and standard-math fixes used by shared animation and particle consumers. `prepareEval` parses once and interprets using fresh temporary variables and current host bindings. The upstream explicit `compile` API is unchanged; it is not the game execution backend and does not gain the interpreter's expanded language coverage.

There is no experimental `compileProgram` API, global program cache or program benchmark in this branch. The interpreter contract and official-language references are in [docs/molang-coverage.md](docs/molang-coverage.md).
