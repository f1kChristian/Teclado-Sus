pluginManagement {
    repositories {
        google()
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
rootProject.name = "TecladoP2PV2"
include(":app", ":core:model", ":core:database", ":core:security", ":core:diagnostics", ":feature:keyboard", ":feature:history", ":feature:settings", ":feature:customization", ":feature:pairing", ":sync", ":webrtc", ":signaling")
