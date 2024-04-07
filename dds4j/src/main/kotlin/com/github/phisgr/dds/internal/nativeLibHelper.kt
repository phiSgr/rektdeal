package com.github.phisgr.dds.internal

import com.github.phisgr.dds.NORTH
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.util.*
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists

private const val PATH_PROP = "java.library.path"

/**
 * Environment variable for configuring the loading of the native library.
 * Defaults to `EXPAND`, the other two options are [LOAD_LIBRARY] and [DISABLE].
 */
private const val DDS4J_LOAD = "DDS4J_LOAD"

/**
 * Environment variable for where to unzip the native library.
 * Used when [DDS4J_LOAD] is `EXPAND` or not set.
 * Defaults to `~/.dds4j`.
 */
private const val DDS4J_DIR = "DDS4J_DIR"

/**
 * Option for when the library is already in [PATH_PROP].
 */
private const val LOAD_LIBRARY = "LOAD_LIBRARY"

/**
 * With this option, the user is responsible for loading the library
 * by calling [System.load] or [System.loadLibrary].
 */
private const val DISABLE = "DISABLE"

internal fun tryLoadLib() = when (val loadOption = System.getenv(DDS4J_LOAD)) {
    // default option, we extract the DDS library from jar and load it
    null, "EXPAND" -> expandAndLoadNativeLib()
    LOAD_LIBRARY -> System.loadLibrary("dds")
    DISABLE -> {}
    else -> System.err.println("Unknown option `$loadOption` for $DDS4J_LOAD")
}

// io.netty.util.internal.PlatformDependent.normalize
private fun String.normalize() = lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "")

// See io.netty.util.internal.NativeLibraryLoader
private fun expandAndLoadNativeLib() {
    fun instructions() =
        "You can set the env var $DDS4J_LOAD=$LOAD_LIBRARY and " +
            "put the native library ${System.mapLibraryName("dds")} in $PATH_PROP: " +
            System.getProperty(PATH_PROP)

    // io.netty.util.internal.PlatformDependent.normalizeOs
    val os = System.getProperty("os.name").normalize().run {
        when {
            startsWith("macosx") || startsWith("osx") || startsWith("darwin") -> "osx"
            startsWith("windows") -> "windows"
            startsWith("linux") -> "linux"
            else -> throw UnsupportedOperationException("Unsupported OS.\n${instructions()}")
        }
    }

    // io.netty.util.internal.PlatformDependent.normalizeArch
    val arch = System.getProperty("os.arch").normalize().run {
        when {
            matches(Regex("^(x8664|amd64|ia32e|em64t|x64)$")) -> "x86_64"
            this == "aarch64" -> "aarch64"
            else -> throw UnsupportedOperationException("Unsupported arch.\n${instructions()}")
        }
    }

    val libToLoad = when ("$os $arch") {
        "windows x86_64" -> "dds.dll"
        "osx x86_64" -> "libdds_intel.dylib"
        "osx aarch64" -> "libdds_arm.dylib"
        "linux x86_64" -> "libdds.so"
        else -> throw UnsupportedOperationException("Unsupported OS arch combination.\n${instructions()}")
    }
    val classLoader = NORTH::class.java.classLoader

    val dir = when (val dir = System.getenv(DDS4J_DIR)) {
        null -> File(System.getProperty("user.home")).resolve(".dds4j")
        else -> File(dir)
    }.also {
        it.mkdirs()
    }

    val file = dir.resolve(System.mapLibraryName("dds"))

    if (!md5Same(file, classLoader, libToLoad)) {
        file.toPath().run {
            deleteIfExists()
            createFile()
        }

        val libStream = classLoader.getResourceAsStream(libToLoad)
            ?: throw IllegalStateException("Native DDS library not found in jar.")

        libStream.use {
            file.outputStream().use { libStream.transferTo(it) }
        }
    }

    System.load(file.toString())
}

/**
 * @return true if the same; false if not exist or different
 */
private fun md5Same(file: File, classLoader: ClassLoader, fileName: String): Boolean {
    val md = MessageDigest.getInstance("MD5")

    try {
        file.inputStream()
    } catch (e: FileNotFoundException) {
        return false
    }.use { existingFile ->
        val buffer = ByteArray(8192)
        var count: Int

        while (existingFile.read(buffer).also { count = it } > 0) {
            md.update(buffer, 0, count)
        }

        val checkSumFile = classLoader.getResourceAsStream("$fileName.md5")
            ?: throw IllegalStateException("DDS library checksum not found in jar.")
        val expected = checkSumFile.use { String(it.readNBytes(32)) }

        return HexFormat.of().parseHex(expected).contentEquals(md.digest())
    }
}
