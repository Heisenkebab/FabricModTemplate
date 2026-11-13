plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.3"

stonecutter parameters {
    // Used as /*$ mod_version*/ and /*$ minecraft*/ in Java sources
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
}
