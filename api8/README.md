This subproject is an unholy workaround.

We want the [ReKtDeal](../rektdeal) library to generate 1.8 classes
so that [Kotlin Jupyter](https://github.com/Kotlin/kotlin-jupyter)
can run the inline functions.

But the following failed:

1. In the same project, compile the Java code (from `jextract`) with 21,
   and Kotlin code with 1.8.
   This is disallowed with the error message
   `Inconsistent JVM-target compatibility detected`.
2. Compile the `dds4j` project with Java 21,
   then depend on it in the `rektdeal` project.
   With `jvmTarget` set to 1.8, the compiler can't see the `dds4j` classes.

This subproject contains dummy classes
(with the same interface as `dds4j`, but compiled with `jvmTarget = "1.8"`)
so that `rektdeal` can compile against them.
