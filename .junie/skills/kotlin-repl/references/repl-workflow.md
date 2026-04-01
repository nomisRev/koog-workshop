# Kotlin REPL Workflow

Use a REPL when you need to validate a small Kotlin/JVM idea against compiled project outputs.

## 1. Pick a JVM target

- Choose the smallest JVM-capable module or source set that contains the code.
- For multiplatform code, compile a concrete JVM target such as `jvmMain`, desktop, or server. Do not point the REPL at `commonMain` alone.
- Skip the REPL for Native-only or JS-only code.

## 2. Refresh compiled outputs

- Compile the target before launching the REPL so you are testing current outputs.
- Use the project's normal build tool. Prefer the narrowest task that refreshes the code you need.
- Gradle examples: `:module:compileKotlin`, `:module:compileKotlinJvm`, `:module:classes`
- Maven example: `mvn -pl <module> -DskipTests compile`

## 3. Assemble the classpath

- Put compiled module outputs first.
- Add project modules only when the snippet touches them.
- Add resources only when the directory exists and the snippet needs them.
- Add dependency jars using build-tool output or IDE metadata rather than guessing file paths manually.

Common output roots:

- Gradle JVM: `<module>/build/classes/kotlin/main`
- Gradle multiplatform JVM: `<module>/build/classes/kotlin/jvm/main`
- Java interop: `<module>/build/classes/java/main`
- Gradle resources: `<module>/build/resources/main`
- Maven JVM: `<module>/target/classes`

## 4. Launch the session

- Start `kotlinc -Xrepl` with the assembled classpath.
- Keep REPL state inside the current project by setting `HOME` and `-J-Duser.home` to a build-owned directory.
- Resolve `<skill-dir>` in the example below to the directory that contains this skill.
- If this skill's launcher script is available, prefer it once you know the classpath.
- Keep the first input small: imports, one helper function, one assertion or print.
- Use `println` and small sample values to confirm behavior.
- Reset the session when switching to a different experiment.

Example:

```bash
CLASSPATH="$PWD/<module>/build/classes/kotlin/main"
STATE_DIR="$PWD/build/codex-repl"

bash <skill-dir>/scripts/start-repl.sh --classpath "$CLASSPATH" --state-dir "$STATE_DIR"
```

## 5. Session discipline

- Test one hypothesis per session.
- Narrow the input shape before changing the code path.
- If the session becomes noisy, stop and restart with a smaller classpath.
- Record the exact snippet that proved the behavior before translating it into source.
