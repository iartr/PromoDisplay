pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PromoDisplay"
val coreDir = "core"
val featureDir = "feature"

include(":app")

// TODO Пример модулей, выпилить из кодовой базы
addCoreModule("somecore")
addFeatureApiImplModule("auto-boot-sample")
addFeatureApiImplModule("ad-source")
fun addCoreModule(moduleName: String) {
    includeModule(moduleName, coreDir)
}

fun addFeatureApiImplModule(moduleName: String) {
    includeApiImpl(moduleName, featureDir)
}

fun includeApiImpl(
    name: String,
    rootDirectory: String,
) {
    includeModule("$name-api", rootDirectory)
    includeModule("$name-impl", rootDirectory)
}

fun includeModule(
    name: String,
    rootDirectory: String,
) {
    val moduleName = ":$rootDirectory:$name"
    val modulePath = listOfNotNull(rootDirectory, name).joinToString(separator = "/")
    include(moduleName)
    project(moduleName).projectDir = File(modulePath)
}

