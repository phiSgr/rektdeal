package com.github.phisgr.rektdeal

import com.github.phisgr.dds.C
import com.github.phisgr.dds.N
import com.github.phisgr.dds.S
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class TestContract {
    private fun testContract(contractString: String, vulScores: List<Int>, nonVulScores: List<Int>) {
        val contract = Contract(contractString)
        assertEquals(contract.toString(), contractString.uppercase().replace("NT", "N"))

        listOf(vulScores to true, nonVulScores to false).forEach { (scores, vul) ->
            assertEquals(
                scores,
                (0..13).map { tricks -> contract.score(tricks, vul) }
            )
        }
    }


    @Test
    fun testScore() {
        testContract(
            "3N",
            listOf(-900, -800, -700, -600, -500, -400, -300, -200, -100, 600, 630, 660, 690, 720),
            listOf(-450, -400, -350, -300, -250, -200, -150, -100, -50, 400, 430, 460, 490, 520)
        )

        testContract(
            "1ntxx",
            listOf(-4000, -3400, -2800, -2200, -1600, -1000, -400, 760, 1160, 1560, 1960, 2360, 2760, 3160),
            listOf(-3400, -2800, -2200, -1600, -1000, -600, -200, 560, 760, 960, 1160, 1360, 1560, 1760)
        )

        testContract(
            "6dX",
            listOf(-3500, -3200, -2900, -2600, -2300, -2000, -1700, -1400, -1100, -800, -500, -200, 1540, 1740),
            listOf(-3200, -2900, -2600, -2300, -2000, -1700, -1400, -1100, -800, -500, -300, -100, 1090, 1190),
        )

        testContract(
            "7H",
            listOf(-1300, -1200, -1100, -1000, -900, -800, -700, -600, -500, -400, -300, -200, -100, 2210),
            listOf(-650, -600, -550, -500, -450, -400, -350, -300, -250, -200, -150, -100, -50, 1510),
        )

        testContract(
            "2S",
            listOf(-800, -700, -600, -500, -400, -300, -200, -100, 110, 140, 170, 200, 230, 260),
            listOf(-400, -350, -300, -250, -200, -150, -100, -50, 110, 140, 170, 200, 230, 260),
        )
    }

    @Test
    fun testIllegalInput() {
        assertThrows<IllegalArgumentException> { Contract("8H") }
        assertThrows<IllegalArgumentException> { Contract(-1, C, doubled = 0) }
        assertThrows<IllegalArgumentException> { Contract(3, C, doubled = 3) }
        assertThrows<IllegalArgumentException> { Contract(8, N, doubled = 0) }
        assertThrows<IllegalArgumentException> { Contract(4, S, doubled = -1) }
        assertThrows<IllegalArgumentException> { Contract("3CXXX") }
        assertThrows<IllegalArgumentException> { Contract("1C").score(tricks = 14, vulnerable = false) }
        assertThrows<IllegalArgumentException> { Contract("1N").score(tricks = -1, vulnerable = false) }
    }
}
