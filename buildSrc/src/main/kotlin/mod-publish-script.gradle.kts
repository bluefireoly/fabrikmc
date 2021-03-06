import BuildConstants.curseforgeId
import BuildConstants.fabrikVersion
import BuildConstants.githubRepo
import BuildConstants.isSnapshot
import BuildConstants.minecraftVersion
import BuildConstants.projectState
import com.matthewprenger.cursegradle.CurseProject
import com.matthewprenger.cursegradle.CurseRelation
import com.matthewprenger.cursegradle.CurseUploadTask
import com.matthewprenger.cursegradle.Options
import com.modrinth.minotaur.TaskModrinthUpload
import net.fabricmc.loom.task.RemapJarTask

plugins {
    kotlin("jvm")

    id("fabric-loom")
    id("com.matthewprenger.cursegradle")
    id("com.modrinth.minotaur")

    `maven-publish`
    signing
}

tasks {
    val curseTasks = withType<CurseUploadTask> {
        dependsOn(tasks.withType<RemapJarTask>())
    }

    val modrinthTask = create<TaskModrinthUpload>("uploadModrinth") {
        group = "upload"
        token = findProperty("modrinth.token").toString()
        projectId = "aTaCgKLW"
        versionNumber = fabrikVersion
        uploadFile = tasks.named("remapJar").get()
        addGameVersion(minecraftVersion)
        addLoader("fabric")
        releaseType = projectState
    }

    create("publishAndUploadMod") {
        group = "upload"
        dependsOn(curseTasks)
        dependsOn(modrinthTask)
        dependsOn(tasks.getByName("publish"))
    }
}

curseforge {
    apiKey = findProperty("curseforge.token") ?: ""
    project(closureOf<CurseProject> {
        mainArtifact(tasks.getByName("remapJar").outputs.files.first())

        id = curseforgeId
        releaseType = projectState
        addGameVersion(minecraftVersion)

        relations(closureOf<CurseRelation> {
            requiredDependency("fabric-api")
            requiredDependency("fabric-language-kotlin")
            // TODO add the following if the projects are separated on curseforge
            /*if (project.name != "fabrikmc-core") {
                requiredDependency("fabrikmc-core")
            }*/
        })
    })
    options(closureOf<Options> {
        forgeGradleIntegration = false
    })
}

publishing {
    repositories {
        maven {
            name = "ossrh"
            credentials(PasswordCredentials::class)
            setUrl(
                if (!isSnapshot)
                    "https://oss.sonatype.org/service/local/staging/deploy/maven2"
                else
                    "https://oss.sonatype.org/content/repositories/snapshots"
            )
        }
    }

    publications {
        create<MavenPublication>(project.name) {
            val remapJarTask = tasks.named("remapJar").get()
            artifact(remapJarTask) {
                builtBy(remapJarTask)
            }
            artifact(tasks.named("sourcesJar").get()) {
                builtBy(tasks.named("remapSourcesJar").get())
            }
            artifact(tasks.getByName("javadocJar"))
            artifact(tasks.jar)

            this.groupId = project.group.toString()
            this.artifactId = project.name
            this.version = fabrikVersion

            pom {
                name.set(project.name)
                description.set(project.description)

                developers {
                    developer {
                        name.set("bluefireoly")
                    }
                }

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                url.set("https://github.com/${githubRepo}")

                scm {
                    connection.set("scm:git:git://github.com/${githubRepo}.git")
                    url.set("https://github.com/${githubRepo}/tree/main")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications)
}
