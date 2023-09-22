package com.github.phisgr.rektdeal

import com.github.phisgr.dds.C
import com.github.phisgr.dds.N
import com.github.phisgr.dds.S
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class TestContract {
    @Test
    fun testScore() {
        val vul = listOf(true, false)
        Contract("3N").let { contract ->
            assertEquals(
                listOf(
                    -900, -450, -800, -400, -700, -350, -600, -300, -500,
                    -250, -400, -200, -300, -150, -200, -100, -100, -50,
                    600, 400, 630, 430, 660, 460, 690, 490, 720, 520,
                ),
                (0..13).flatMap { tricks ->
                    vul.map { vul ->
                        contract.score(tricks, vul)
                    }
                }
            )
        }

        Contract("1NXX").let { contract ->
            assertEquals(
                listOf(
                    -4000, -3400, -3400, -2800, -2800, -2200, -2200, -1600, -1600, -1000, -1000, -600, -400, -200,
                    760, 560, 1160, 760, 1560, 960, 1960, 1160, 2360, 1360, 2760, 1560, 3160, 1760
                ),
                (0..13).flatMap { tricks ->
                    vul.map { vul ->
                        contract.score(tricks, vul)
                    }
                }
            )
        }


        Contract("6DX").let { contract ->
            assertEquals(
                listOf(
                    -3500, -3200, -3200, -2900, -2900, -2600, -2600, -2300, -2300, -2000,
                    -2000, -1700, -1700, -1400, -1400, -1100, -1100, -800, -800, -500,
                    -500, -300, -200, -100, 1540, 1090, 1740, 1190
                ),
                (0..13).flatMap { tricks ->
                    vul.map { vul ->
                        contract.score(tricks, vul)
                    }
                })
        }


        Contract("7H").let { contract ->
            assertEquals(
                listOf(
                    -1300, -650, -1200, -600, -1100, -550, -1000, -500, -900, -450, -800, -400, -700, -350,
                    -600, -300, -500, -250, -400, -200, -300, -150, -200, -100, -100, -50,
                    2210, 1510
                ),
                (0..13).flatMap { tricks ->
                    vul.map { vul ->
                        contract.score(tricks, vul)
                    }
                })
        }

        Contract("2S").let { contract ->
            assertEquals(
                listOf(
                    -800, -400, -700, -350, -600, -300, -500, -250, -400, -200, -300, -150, -200, -100, -100, -50,
                    110, 110, 140, 140, 170, 170, 200, 200, 230, 230, 260, 260
                ),
                (0..13).flatMap { tricks ->
                    vul.map { vul ->
                        contract.score(tricks, vul)
                    }
                })
        }
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
