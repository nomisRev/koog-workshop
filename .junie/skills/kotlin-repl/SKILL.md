---
name: kotlin-repl
description: Launch and use a Kotlin/JVM REPL against a real project classpath to prototype snippets, inspect runtime behavior, validate imports and overloads, and debug small pieces of Kotlin code before editing source files or writing tests. Use when Codex needs to check a Kotlin hypothesis against compiled project outputs, framework APIs, or dependency resolution in a live REPL.
---

# Kotlin REPL

Use this skill to validate small Kotlin/JVM ideas against compiled project outputs before editing source files.

## Workflow

1. Pick the narrowest JVM-capable module or target that covers the code under test.
2. Refresh compiled outputs first so the REPL sees current classes and resources.
3. Assemble a minimal classpath: module outputs first, then only the project modules and dependency jars the snippet needs.
4. Launch the REPL, prove one hypothesis, and record the exact snippet that worked.
5. Translate the validated snippet into source code or a test.

## Scope Rules

- Use this skill for Kotlin/JVM code or a JVM target of a multiplatform project.
- If the code lives in `commonMain`, still compile and load a concrete JVM target before using the REPL.
- Prefer a test or small runnable program instead when behavior depends on long flows, concurrency, or stable assertions across many cases.

## Classpath Rules

- Prefer compiled outputs over raw source directories.
- Put the module under test first on the classpath.
- Add resource directories only when they exist and the snippet needs them.
- Add dependency jars from the build tool or IDE model rather than guessing jar paths manually.
- Keep the classpath minimal so startup stays fast and symbol resolution stays clear.

## Launch Rules

- Launch the session with `kotlinc -Xrepl`, not `kotlin -Xrepl`.
- Keep REPL state inside the current project by setting `-J-Duser.home` to a build-owned directory such as `build/codex-repl` or `<module>/build/codex-repl`.
- Resolve `<skill-dir>` in the examples below to the directory that contains this `SKILL.md`.
- Use `bash` with [scripts/start-repl.sh](scripts/start-repl.sh) when you already know the classpath. It handles the `HOME` and `user.home` boilerplate consistently.
- If you need `kotlin.uuid.Uuid.parse(...)` or similar experimental APIs in the REPL, put the code inside a helper function annotated with `@OptIn(...)`; top-level opt-in annotations may not carry the way you expect between REPL snippets.

## Gradle JVM Example

```bash
MODULE=app
BUILD_TASK=":$MODULE:compileKotlin"
CLASSPATH="$PWD/$MODULE/build/classes/kotlin/main"
STATE_DIR="$PWD/build/codex-repl"

./gradlew "$BUILD_TASK"
bash <skill-dir>/scripts/start-repl.sh --classpath "$CLASSPATH" --state-dir "$STATE_DIR"
```

Then start with a small probe:

```kotlin
import your.pkg.TypeUnderTest

fun demoValue(): TypeUnderTest = TODO("construct the smallest representative value")
println(demoValue())
```

## Good REPL Uses

- Verify how a library call behaves on the real project classpath.
- Check extension resolution, nullability, and overload selection.
- Inspect service outputs before changing a repository or UI layer.
- Prototype a transformation and then carry the working snippet back into production code.

## Avoid

- Large end-to-end flows that are clearer as tests.
- Experiments that need deterministic assertions across many cases.
- Repeating the same snippet without narrowing the inputs or the classpath.

## Details

See [references/repl-workflow.md](references/repl-workflow.md) for the concrete launch pattern and session discipline.
