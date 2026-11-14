package app.lucent.firefly.core.network

/**
 * Relative API paths. They must not start with `/`: the client's default URL ends with `/` and
 * a leading slash would replace its path, breaking servers installed under a sub-path.
 */
enum class ApiVersion(
    val path: String,
) {
    V1("api/v1"),
    ;

    operator fun invoke(route: String = ""): String = if (route.isEmpty()) path else "$path/${route.trimStart('/')}"
}
