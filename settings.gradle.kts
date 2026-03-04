pluginManagement {
    repositories {
        // 阿里云镜像 - Gradle 插件
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        // 阿里云镜像 - Google
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 阿里云镜像 - Maven Central
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        // 阿里云镜像 - 公共仓库
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云镜像 - Google
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // 阿里云镜像 - Maven Central
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        // 阿里云镜像 - 公共仓库
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
    }
}

rootProject.name = "BallGame"
include(":app")
