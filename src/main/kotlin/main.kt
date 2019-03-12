package com.code.gamerg8.nami

import com.code.gamerg8.nami.vulkan.Vertex
import org.joml.Vector2f
import org.joml.Vector3f

fun main(args: Array<String>) {
    Vulkan.run()

    // TODO: This is a placeholder
    val vertexArray: Array<Vertex> = arrayOf(
        Vertex(Vector2f(0.0f, -0.5f), Vector3f(1.0f, 0.0f, 0.0f)),
        Vertex(Vector2f(0.5f, 0.5f), Vector3f(0.0f, 1.0f, 0.0f)),
        Vertex(Vector2f(-0.5f, 0.5f), Vector3f(0.0f, 0.0f, 1.0f))
    )
}