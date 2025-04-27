package com.github.phisgr.rektdeal.internal


fun List<Int>.permutations(): Sequence<List<Int>> = sequence {
    if (isEmpty()) yield(emptyList())
    else if (size == 1) yield(this@permutations)
    else {
        // reusing the sub-lists isn't great for readability
        val copy = ArrayList(subList(1, size))
        forEachIndexed { index, i ->
            val res = ArrayList<Int>(size)
            res.add(i)
            repeat(size - 1) { res.add(0) }

            copy.permutations().forEach {
                repeat(size - 1) { i -> res[i + 1] = it[i] }
                yield(res)
            }
            if (index < lastIndex) {
                copy[index] = this@permutations[index]
            }
        }
    }
}

fun List<List<Int>>.nestingPermutations(): Sequence<List<Int>> = sequence {
    if (isEmpty()) yield(emptyList())
    else first().permutations().forEach { firstPerm ->
        subList(1, size).nestingPermutations().forEach { restPerm ->
            yield(firstPerm + restPerm)
        }
    }
}

fun List<Int>.substituteWildCard(remaining: Int, wildcardCount: Int): Sequence<List<Int>> = sequence {
    if (wildcardCount == 0) {
        yield(this@substituteWildCard)
    } else {
        val index = indexOf(-1)
        if (wildcardCount == 1) {
            yield(this@substituteWildCard.toMutableList().also {
                it[index] = remaining
            })
        } else {
            val prefix = subList(0, index)
            for (i in 0..remaining) {
                subList(index + 1, size).substituteWildCard(
                    remaining = remaining - i,
                    wildcardCount = wildcardCount - 1
                ).forEach {
                    yield(ArrayList<Int>(size).apply {
                        addAll(prefix)
                        add(i)
                        addAll(it)
                    })
                }
            }
        }
    }
}

/**
 * See [combinatorial number system](https://en.wikipedia.org/wiki/Combinatorial_number_system)
 */
fun flatten(s: Int, h: Int, d: Int): Int {
    val second = s + h + 1
    val third = second + d + 1

    return s + second * (second - 1) / 2 + third * (third - 1) * (third - 2) / 6
}
