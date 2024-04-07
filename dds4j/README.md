# DDS4J

Wrapper around the C++ library [DDS](https://github.com/dds-bridge/dds).

This project makes use of two code-generators -
[jextract](https://jdk.java.net/jextract/) for the low level Java FFM code, and
a [custom one](../buildSrc/src/main/kotlin/com/github/phisgr/dds/GenerateTask.kt)
to generate higher level wrapper classes and functions.

This library is written in Kotlin,
but the "4J" name should tell you that Java code can use it as well. [^1]

[^1]: In earlier iterations, the enum-like types
`Strain`, `Suit`, `Seats`, and `Direction` are written in Kotlin.
But using them in Java is awkward, so they were moved to Java files.

## Usage

```kotlin
implementation("com.github.phisgr:dds4j:0.1.0")
```

### Type Safety

The classes from `jextract` are in package `dds`,
you do not need to use them directly.
That's what using them look like:

<details>
<summary>Using raw generated code</summary>

```java
MemorySegment deal = dealPBN.allocate(arena);
dealPBN.trump(deal, 0);
dealPBN.first(deal, 0);
dealPBN.currentTrickSuit(deal).fill((byte) 0);
dealPBN.currentTrickRank(deal).fill((byte) 0);
dealPBN.remainCards(deal)
        .setString(0, "N:QJ6.K652.J85.T98 873.J97.AT764.Q4 K5.T83.KQ9.A7652 AT942.AQ4.32.KJ3");

MemorySegment ftMemory = futureTricks.allocate(arena);
int returnCode = SolveBoardPBN(deal, -1, 1, 1, ftMemory, 0);
if (returnCode != 1) throw new IllegalArgumentException(STR."Got return code \{returnCode}");

int trickCount = 13 - futureTricks.score(ftMemory, 0);

System.out.println(STR."The double dummy tricks for declarer is \{trickCount}" );
```

</details>

And this is the equivalent using DDS4J wrapper code:

```java
DealPBN deal = new DealPBN(arena);
deal.setTrump(Strain.S);
deal.setFirst(Direction.N);
deal.getCurrentTrickSuit().clear();
deal.getCurrentTrickRank().clear();
deal.setRemainCards("N:QJ6.K652.J85.T98 873.J97.AT764.Q4 K5.T83.KQ9.A7652 AT942.AQ4.32.KJ3");

FutureTricks futureTricks = new FutureTricks(arena);
solveBoardPBN(deal, -1, 1, 1, futureTricks, 0);

System.out.println(futureTricks);
System.out.println(STR."The double dummy tricks for declarer is \{13 - futureTricks.getScore().get(0)}" );
```

Everything is well typed, you will not make the silly mistake of
passing a `deal` memory segment (which stores the cards in binary) to `solveBoardPBN`.
It also checks the return code for you.

<details>
<summary>And of course it's sweeter if you use Kotlin</summary>

```kotlin
val deal = DealPBN(arena)
deal.trump = S // property access syntax
deal.first = NORTH
deal.currentTrickSuit.memory.clear()
deal.currentTrickRank.memory.clear()
deal.remainCards = "N:QJ6.K652.J85.T98 873.J97.AT764.Q4 K5.T83.KQ9.A7652 AT942.AQ4.32.KJ3"

val futureTricks = FutureTricks(arena)
solveBoardPBN(deal, target = -1, solutions = 1, mode = 1, futureTricks, thrId = 0) // named arguments

println(futureTricks)
println("The double dummy tricks for declarer is ${13 - futureTricks.score[0]}") // indexed access operator
```

</details>

### Thread Safety

In the examples above, you can see a final argument of `0` passed to `solveBoardPBN`.
This is the `threadIndex`. DDS pre-allocates the data structures for solving the boards,
the `threadIndex` argument specifies which one to use.
Some functions (e.g. `calcDdTable`) launch multiple threads and use all those data structures.
If you concurrently use the same data in two threads, *very bad things*™ can happen.

To prevent that, the methods in the `com.github.phisgr.dds.Dds` class
acquire the correct lock(s) before executing native methods.

### Documentation

Since this is just a thin wrapper, I hope the [DDS documentation](
https://github.com/dds-bridge/dds/blob/develop/doc/dll-description.md) will suffice.
For example, you can refer to it for the meanings of the
`target`, `solutions`, and `mode` arguments above.

## Custom Binary

If your system is not any of the supported systems,
you can build your own binary from source.

To make DDS4J load your binary,
set the environment variable `DDS4J_LOAD` to `LOAD_LIBRARY`.
Then put the dynamic library in any of the `java.library.path`,
e.g. the working directory.

Or you can set `DDS4J_LOAD` to `DISABLE`,
and call `System.loadLibrary("dds")` or `System.load(absolutePath)`
before you call the methods in `com.github.phisgr.dds.Dds`.
