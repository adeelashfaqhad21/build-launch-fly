package com.buildlaunchfly.game.gfx

import com.buildlaunchfly.game.math.M
import com.buildlaunchfly.game.math.Mat4
import com.buildlaunchfly.game.math.Vec3

/** Perspective camera with cached view / projection / inverse matrices. */
class Camera {

    val position = Vec3(0f, 10f, 20f)
    val target = Vec3(0f, 0f, 0f)

    var fovYDegrees = 58f
    var aspect = 16f / 9f
    var near = 0.2f
    var far = 2400f

    val view = Mat4()
    val projection = Mat4()
    val viewProj = Mat4()
    val invViewProj = Mat4()

    val forward = Vec3(0f, 0f, -1f)
    val right = Vec3(1f, 0f, 0f)
    val up = Vec3(0f, 1f, 0f)

    private val worldUp = Vec3(0f, 1f, 0f)

    fun update() {
        view.setLookAt(position, target, worldUp)
        projection.setPerspective(M.deg(fovYDegrees), aspect, near, far)
        viewProj.setProduct(projection, view)
        invViewProj.set(viewProj).invert()

        forward.set(target).sub(position)
        if (forward.lengthSq() > 1e-8f) {
            forward.normalize()
        } else {
            forward.set(0f, 0f, -1f)
        }
        right.cross(forward, worldUp)
        if (right.lengthSq() > 1e-8f) {
            right.normalize()
        } else {
            right.set(1f, 0f, 0f)
        }
        up.cross(right, forward).normalize()
    }

    fun setViewport(width: Int, height: Int) {
        aspect = if (height > 0) width.toFloat() / height.toFloat() else 16f / 9f
    }
}
