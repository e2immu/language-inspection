tasks.register("publish") {
    dependsOn(gradle.includedBuild("e2immu-inspection-api").task(":publish"))
    dependsOn(gradle.includedBuild("e2immu-inspection-integration").task(":publish"))
    dependsOn(gradle.includedBuild("e2immu-inspection-resource").task(":publish"))
    dependsOn(gradle.includedBuild("e2immu-inspection-parser").task(":publish"))
}
