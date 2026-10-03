// Root build file — plugin versions are declared in app/build.gradle.kts.
tasks.register("clean", Delete::class) {
  delete(rootProject.layout.buildDirectory)
}
