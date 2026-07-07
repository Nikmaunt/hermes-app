import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/**
 * `hermes.quality` — detekt + ktlint on every module. Both run under
 * `./gradlew check`, so style and static-analysis failures fail the build.
 */
class HermesQualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("io.gitlab.arturbosch.detekt")
            apply("org.jlleitschuh.gradle.ktlint")
        }

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            parallel = true
            // A shared detekt config lives at the repo root; use it when present.
            val shared = rootProject.file("config/detekt/detekt.yml")
            if (shared.exists()) config.setFrom(shared)
        }

        extensions.configure<KtlintExtension> {
            android.set(true)
            ignoreFailures.set(false)
        }
    }
}
