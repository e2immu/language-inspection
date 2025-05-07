tasks.register("test") {
    dependsOn(gradle.includedBuild("e2immu-external-support").task(":test"))
//    dependsOn(gradle.includedBuild("e2immu-internal-graph").task(":test"))
    dependsOn(gradle.includedBuild("e2immu-cst-impl").task(":test"))
    dependsOn(gradle.includedBuild("e2immu-cst-io").task(":test"))
    dependsOn(gradle.includedBuild("e2immu-cst-print").task(":test"))
    dependsOn(gradle.includedBuild("e2immu-java-parser").task(":test"))
    dependsOn(gradle.includedBuild("e2immu-java-bytecode").task(":test"))
    dependsOn(gradle.includedBuild("e2immu-inspection-integration").task(":test"))
}
tasks.register("publish") {
    dependsOn(gradle.includedBuild("e2immu-inspection-api").task(":publish"))
    dependsOn(gradle.includedBuild("e2immu-inspection-integration").task(":publish"))
    dependsOn(gradle.includedBuild("e2immu-inspection-resource").task(":publish"))
    dependsOn(gradle.includedBuild("e2immu-inspection-parser").task(":publish"))
}
tasks.register("publishToMavenLocal") {
    dependsOn(gradle.includedBuild("e2immu-inspection-api").task(":publishToMavenLocal"))
    dependsOn(gradle.includedBuild("e2immu-inspection-integration").task(":publishToMavenLocal"))
    dependsOn(gradle.includedBuild("e2immu-inspection-resource").task(":publishToMavenLocal"))
    dependsOn(gradle.includedBuild("e2immu-inspection-parser").task(":publishToMavenLocal"))
}
