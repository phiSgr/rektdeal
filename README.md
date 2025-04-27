# ReKtDeal

## A Kotlin reimplementation of Antony Lee's Redeal,

### which is a reimplementation of Thomas Andrews' Deal in Python.

[Redeal](https://github.com/anntzer/redeal) is a deal generator:
it outputs deals satisfying whatever conditions you specify -
deals with a double void, deals with a strong 2♣️ opener opposite a yarborough, etc.
Using Bo Haglund's double dummy solver,
it can even solve the hands it has generated for you.
Unfortunately, the language of Redeal - Python - is slow.
ReKtDeal is thus my rewrite of Redeal using another language: Kotlin.

The deal generation in ReKtDeal is often 100x faster than Redeal,
and that's before multi-threading.

ReKtDeal runs on JVM 22+.
The [embedded double dummy solver](./dds4j) is pre-built for
Windows/Linux x86_64, macOS x86_64/Apple Silicon.
I believe this covers most users.
See the [instructions](./dds4j#custom-binary) if your system is not included.

The introduction paragraph was adapted from the [README of Redeal](
https://github.com/anntzer/redeal/blob/main/README.rst).
The almost identical wording was inspired by some then-recent news.

## Setup

Fundamentally, ReKtDeal is just a Kotlin library.
Using it is no different from any other library from Maven.

<details>
  <summary>E.g. Gradle (Kotlin DSL)</summary>

```kotlin
dependencies {
    implementation("com.github.phisgr:rektdeal:0.2.0")
}

// Set the JVM args
application {
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}
```

</details>

But I imagine most uses are going to be interactive.
For that [Kotlin Jupyter](https://github.com/Kotlin/kotlin-jupyter) can be used.

```kotlin
@file:DependsOn("com.github.phisgr:rektdeal:0.2.0")
```

# Talk is cheap. Show me the code.

See a full introduction demo [here](examples/introduction.ipynb).
