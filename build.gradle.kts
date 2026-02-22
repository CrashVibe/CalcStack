plugins {
  kotlin("jvm") version "2.3.20-Beta1"
  id("com.gradleup.shadow") version "8.3.0"
  id("xyz.jpenilla.run-paper") version "2.3.1"
}

version = properties["version"].toString()
group = properties["group"].toString()
description = properties["description"].toString()

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
  implementation("de.exlll:configlib-yaml:4.6.1")
  implementation("org.bstats:bstats-bukkit:3.1.0")
  compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
}


kotlin {
  jvmToolchain(21)
}


tasks {
  runServer {
    // Configure the Minecraft version for our task.
    // This is the only required configuration besides applying the plugin.
    // Your plugin's jar (or shadowJar if present) will be used automatically.
    minecraftVersion("1.21.8")
  }

  register("package") {
    dependsOn(clean, shadowJar)
    val outputDir = rootDir.resolve("outputs")
    outputDir.mkdirs()
    doLast {
      val shadowJarFile = shadowJar.get().outputs.files.singleFile
      shadowJarFile.copyTo(outputDir.resolve(shadowJarFile.name), overwrite = true)
    }
  }

  shadowJar {
    archiveFileName = "${rootProject.name}-paper-${project.version}.${archiveExtension.get()}"
    exclude("META-INF/**")
    relocate("org.bstats", "${project.group}.libs.bstats")
  }

  processResources {
    filesMatching("**/plugin.yml") {
      expand(
        "name" to rootProject.name,
        "version" to project.version,
        "description" to project.description
      )
    }
  }

  withType<JavaCompile> {
    options.encoding = "UTF-8"
  }
}
