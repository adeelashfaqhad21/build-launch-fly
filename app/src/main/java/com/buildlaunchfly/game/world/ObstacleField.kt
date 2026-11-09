package com.buildlaunchfly.game.world

import kotlin.math.floor

/**
 * Everything solid in a world, in one lookup the flight model can ask every frame.
 *
 * A tree, a boulder, a house and a tower are all approximated by an upright cylinder: a centre
 * on the ground plane, a top and a radius. The top is what makes altitude matter — a low pass
 * through the canopy is a collision, a pull-up that clears the crown is not — and the radius is
 * measured at the widest part, so the player has to steer around timber instead of through it.
 *
 * The cylinders are bucketed into a coarse grid when the world is built, so a query only touches
 * the handful of cells around the aircraft instead of the thousands of trees standing in the
 * world.
 */
class ObstacleField(private val cellSize: Float = 32f) {

    private val xs = ArrayList<Float>(512)
    private val zs = ArrayList<Float>(512)
    private val tops = ArrayList<Float>(512)
    private val radii = ArrayList<Float>(512)

    /** Cell lookup, built by [freeze] once the world is finished (or on the first query). */
    private var grid: HashMap<Long, IntArray>? = null

    /** How many solid cylinders the world registered. */
    val count: Int get() = xs.size

    /** Registers one solid cylinder standing on the ground at ([x], [z]). */
    fun add(x: Float, z: Float, topY: Float, radius: Float) {
        if (radius <= 0f || !topY.isFinite() || !x.isFinite() || !z.isFinite()) return
        xs.add(x)
        zs.add(z)
        tops.add(topY)
        radii.add(radius)
        grid = null
    }

    /** Builds the lookup grid. Called once the world is finished, or on the first query. */
    fun freeze() {
        if (grid == null && xs.isNotEmpty()) buildGrid()
    }

    /**
     * True when a disc of [radius] centred at ([x], [y], [z]) overlaps anything solid: inside a
     * cylinder in plan and below its top.
     */
    fun blocks(x: Float, y: Float, z: Float, radius: Float): Boolean {
        if (xs.isEmpty()) return false

        val map = grid ?: buildGrid()
        val cx = cellOf(x)
        val cz = cellOf(z)
        for (dx in -1..1) {
            for (dz in -1..1) {
                val cell = map[key(cx + dx, cz + dz)] ?: continue
                for (index in cell) {
                    if (y > tops[index]) continue
                    val ox = x - xs[index]
                    val oz = z - zs[index]
                    val reach = radius + radii[index]
                    if (ox * ox + oz * oz <= reach * reach) return true
                }
            }
        }
        return false
    }

    private fun buildGrid(): HashMap<Long, IntArray> {
        val buckets = HashMap<Long, ArrayList<Int>>(xs.size / 2 + 8)
        for (index in xs.indices) {
            // A mountain is wider than a single cell, so it is filed under every cell its
            // footprint touches; otherwise a query could slide past its edge.
            val minX = cellOf(xs[index] - radii[index])
            val maxX = cellOf(xs[index] + radii[index])
            val minZ = cellOf(zs[index] - radii[index])
            val maxZ = cellOf(zs[index] + radii[index])
            for (cellX in minX..maxX) {
                for (cellZ in minZ..maxZ) {
                    buckets.getOrPut(key(cellX, cellZ)) { ArrayList(4) }.add(index)
                }
            }
        }
        val map = HashMap<Long, IntArray>(buckets.size)
        for ((cellKey, list) in buckets) map[cellKey] = list.toIntArray()
        grid = map
        return map
    }

    private fun cellOf(value: Float): Int = floor(value / cellSize).toInt()

    private fun key(cellX: Int, cellZ: Int): Long =
        (cellX.toLong() shl 32) xor (cellZ.toLong() and 0xffffffffL)
}
