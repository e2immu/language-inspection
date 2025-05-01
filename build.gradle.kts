tasks.register("publish") {
    dependsOn(gradle.includedBuild("language-inspection-api").task(":publish"))
    dependsOn(gradle.includedBuild("language-inspection-integration").task(":publish"))
    dependsOn(gradle.includedBuild("language-inspection-resource").task(":publish"))
    dependsOn(gradle.includedBuild("language-inspection-parser").task(":publish"))
}
