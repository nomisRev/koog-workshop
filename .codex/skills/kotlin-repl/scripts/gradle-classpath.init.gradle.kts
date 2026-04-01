val configurationName = gradle.startParameter.projectProperties["codexConfigurationName"] ?: "runtimeClasspath"

allprojects {
    tasks.register("codexListResolvableConfigurations") {
        doLast {
            configurations
                .filter { it.isCanBeResolved }
                .map { it.name }
                .sorted()
                .forEach(::println)
        }
    }

    tasks.register("codexPrintResolvedFiles") {
        doLast {
            val configuration = configurations.findByName(configurationName)
                ?: error(
                    buildString {
                        append("Configuration '")
                        append(configurationName)
                        append("' not found in ")
                        append(path)
                        append(". Available resolvable configurations: ")
                        append(
                            configurations
                                .filter { it.isCanBeResolved }
                                .map { it.name }
                                .sorted()
                                .joinToString(", ")
                        )
                    }
                )

            if (!configuration.isCanBeResolved) {
                error("Configuration '$configurationName' exists in $path but is not resolvable.")
            }

            configuration
                .resolve()
                .map { it.absoluteFile.normalize() }
                .distinct()
                .sortedBy { it.path }
                .forEach { println(it.path.replace('\\', '/')) }
        }
    }
}
