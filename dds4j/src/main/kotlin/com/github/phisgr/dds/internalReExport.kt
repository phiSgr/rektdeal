/**
 * putting static fields/methods in Java in the top level in Kotlin
 */

@file:JvmName("-internalReExport") // invisible in Java code

package com.github.phisgr.dds

val S: Suit = Strain.S
val H: Suit = Strain.H
val D: Suit = Strain.D
val C: Suit = Strain.C

val N: Strain = Strain.N

val SUITS: List<Suit> = Strain.SUITS
fun Int.toSuit(): Suit = Suit.fromInt(this)

val STRAINS: List<Strain> = Strain.STRAINS
fun Int.toStrain(): Strain = Strain.fromInt(this)

/**
 * [ContractType.denom] is encoded differently.
 */
internal fun Int.toContractTypeDenom(): Strain = ((this + 4) % 5).toStrain()

val NORTH: Direction = Seats.N
val EAST: Direction = Seats.E
val SOUTH: Direction = Seats.S
val WEST: Direction = Seats.W

val DIRECTIONS: List<Direction> = Seats.DIRECTIONS
fun Int.toDirection(): Direction = Direction.fromInt(this)
val SEATS: List<Seats> = Seats.SEATS
fun Int.toSeats(): Seats = Seats.fromInt(this)
