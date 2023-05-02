allprojects {
    repositories {
        mavenCentral()
    }

    apply {
        plugin("java")
        plugin("base")
        plugin("idea")
    }
}