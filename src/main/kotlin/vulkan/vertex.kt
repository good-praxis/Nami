package com.code.gamerg8.nami.vulkan

import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkVertexInputAttributeDescription
import org.lwjgl.vulkan.VkVertexInputBindingDescription
import java.nio.ByteBuffer

data class Vertex(val pos: Vector2f, val color: Vector3f) {
    companion object {
        const val BYTE_SIZE = (2+3) * 4 //pos: 2f, color 3f, 4 byte per float

        fun getBindingDescription(): VkVertexInputBindingDescription.Buffer {
            val desc = VkVertexInputBindingDescription.calloc(1)
            desc.binding(0)
            desc.stride(Vertex.BYTE_SIZE)
            desc.inputRate(VK_VERTEX_INPUT_RATE_VERTEX)
            return desc
        }

        fun getAttributeDescription(): VkVertexInputAttributeDescription.Buffer {
            val desc = VkVertexInputAttributeDescription.calloc(2)
            desc[0].binding(0)
            desc[0].location(0)
            desc[0].format(VK_FORMAT_R32G32_SFLOAT)
            desc[0].offset(0)

            desc[1].binding(0)
            desc[1].location(1)
            desc[1].format(VK_FORMAT_R32G32B32_SFLOAT)
            desc[1].offset((2 * 4)) // pos is 2 floats, a float is 4 byte

            return desc
        }

        fun toByteBuffer(vertices: Array<Vertex>, vertexData: ByteBuffer) {
            val data = vertexData.asFloatBuffer()
            for (vertex in vertices) {
                vertex.pos.get(data)
                data.position(data.position()+2)
                vertex.color.get(data)
                data.position(data.position()+3)
            }
        }
    }
}