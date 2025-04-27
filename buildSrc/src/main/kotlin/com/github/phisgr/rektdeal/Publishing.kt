package com.github.phisgr.rektdeal

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get

object Publishing {
    fun PublishingExtension.configure(
        project: Project,
        pomName: String,
        pomDescription: String,
        scmUrl: String,
    ) {

        repositories {
            maven {
                name = "sonatype"
                setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = project.properties["ossrhUsername"] as String?
                    password = project.properties["ossrhPassword"] as String?
                }
            }
        }
        publications {
            create<MavenPublication>("maven") {

                from(project.components["java"])

                pom {
                    name.set(pomName)
                    description.set(pomDescription)
                    url.set(scmUrl)

                    licenses {
                        license {
                            name.set("GPL-v3.0")
                            url.set("http://www.gnu.org/licenses/gpl-3.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("phiSgr")
                            name.set("George Leung")
                            email.set("phisgr@gmail.com")
                        }
                    }
                    scm {
                        url.set(scmUrl)
                    }
                }
            }
        }
    }

}
