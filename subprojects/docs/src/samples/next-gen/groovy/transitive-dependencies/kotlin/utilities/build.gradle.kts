plugins {
    groovy
}

repositories {
    jcenter()
}

dependencies {
    compile(project(":list"))
    implementation("org.codehaus.groovy:groovy-all:2.5.7")
}
