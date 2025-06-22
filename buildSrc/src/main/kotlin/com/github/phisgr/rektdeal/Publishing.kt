package com.github.phisgr.rektdeal

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPomLicense
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get

object Publishing {
    fun PublishingExtension.configure(
        project: Project,
        pomName: String,
        pomDescription: String,
        scmUrl: String,
        licenseSpec: MavenPomLicense.() -> Unit,
    ) {

        repositories {
            maven {
                name = "ossrh-staging-api"
                setUrl("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
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
                            licenseSpec()
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
