package com.code.gamerg8.nami.vulkan

import com.code.gamerg8.nami.Util
import com.code.gamerg8.nami.Util.nullptr
import com.code.gamerg8.nami.Vulkan
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.*
import java.nio.LongBuffer

class Buffers {
    lateinit var swapchainFramebuffers: Array<Long>
    lateinit var commandBuffers: Array<VkCommandBuffer>
    var vertexBuffer: Long = nullptr
    var vertexBufferMemory: Long = nullptr
    var indexBuffer: Long = nullptr
    var indexBufferMemory: Long = nullptr

    fun createFramebuffers() {
        swapchainFramebuffers = Array(Vulkan.device.swapchainImageViews.size) { Util.nullptr }
        for((index, view) in Vulkan.device.swapchainImageViews.withIndex()) {
            val attachments = memAllocLong(1)
            attachments.put(view).flip()

            val framebufferInfo = VkFramebufferCreateInfo.calloc()
            framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
            framebufferInfo.renderPass(Vulkan.pipeline.renderPass)
            framebufferInfo.pAttachments(attachments)
            framebufferInfo.width(Vulkan.device.swapchainExtent.width())
            framebufferInfo.height(Vulkan.device.swapchainExtent.height())
            framebufferInfo.layers(1)

            swapchainFramebuffers[index] = MemoryStack.stackPush().use {
                val pFramebuffer = it.mallocLong(1)
                if(vkCreateFramebuffer(Vulkan.device.logicalDevice, framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                    error("Failed to create framebuffer")
                }
                pFramebuffer[0]
            }

        }
    }

    fun createCommandBuffers() { // TODO: RESEARCH COMMAND BUFFERS
        val allocInfo = VkCommandBufferAllocateInfo.calloc()
        allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
        allocInfo.commandPool(Vulkan.device.commandPool)
        allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
        allocInfo.commandBufferCount(swapchainFramebuffers.size)

        MemoryStack.stackPush().use {
            val commandBufferPointers = it.mallocPointer(swapchainFramebuffers.size)
            if(vkAllocateCommandBuffers(Vulkan.device.logicalDevice, allocInfo, commandBufferPointers) != VK_SUCCESS) {
                error("Failed to allocate command buffers")
            }

            commandBuffers = Array(swapchainFramebuffers.size) { i ->
                VkCommandBuffer(commandBufferPointers[i], Vulkan.device.logicalDevice)
            }
        }

        for((index, commandBuffer) in commandBuffers.withIndex()) {
            val beginInfo = VkCommandBufferBeginInfo.calloc()
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            beginInfo.flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT)
            beginInfo.pInheritanceInfo(null)

            if(vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS)
                error("Failed to begin recording of command buffer")

            val renderPassInfo = VkRenderPassBeginInfo.calloc()
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
            renderPassInfo.renderPass(Vulkan.pipeline.renderPass)
            renderPassInfo.framebuffer(swapchainFramebuffers[index])
            val renderArea = VkRect2D.calloc()
            renderArea.offset(VkOffset2D.calloc().set(0,0))
            renderArea.extent(Vulkan.device.swapchainExtent)
            renderPassInfo.renderArea(renderArea)

            val clearValues = VkClearValue.calloc(1)
            clearValues.color().float32(0, 0f).float32(1, 1f).float32(2, 1f).float32(3, 1f)
            renderPassInfo.pClearValues(clearValues)

            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE)

            // drawing commands
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, Vulkan.pipeline.pipeline)

            val vertexBuffers = memAllocLong(1).put(vertexBuffer).flip() as LongBuffer
            val offsets = memAllocLong(1).put(0).flip() as LongBuffer
            vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets)
            vkCmdBindIndexBuffer(commandBuffer, indexBuffer, 0, VK_INDEX_TYPE_UINT32)

            vkCmdDrawIndexed(commandBuffer, Vulkan.indices.capacity(), 1, 0, 0, 0) // draw 3 vertices (1 instance)

            vkCmdEndRenderPass(commandBuffer)

            if(vkEndCommandBuffer(commandBuffer) != VK_SUCCESS)
                error("Failed to record command buffer")
        }
    }

    private fun createBuffer(size: Long, usage: Int, properties: Int, pBuffer: LongBuffer, pBufferMemory: LongBuffer) {
        val bufferInfo = VkBufferCreateInfo.calloc()
        bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
        bufferInfo.size(size)

        bufferInfo.usage(usage)

        bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE)

        if(vkCreateBuffer(Vulkan.device.logicalDevice, bufferInfo, null, pBuffer) != VK_SUCCESS)
            error("Error while creating vertex buffer")

        val memRequirements = VkMemoryRequirements.calloc()
        vkGetBufferMemoryRequirements(Vulkan.device.logicalDevice, pBuffer[0], memRequirements)

        val allocInfo = VkMemoryAllocateInfo.calloc()
        allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
        allocInfo.allocationSize(memRequirements.size())
        allocInfo.memoryTypeIndex(Vulkan.device.findMemoryType(memRequirements.memoryTypeBits(),
            properties))

        memRequirements.free()

        if(vkAllocateMemory(Vulkan.device.logicalDevice, allocInfo, null, pBufferMemory) != VK_SUCCESS)
            error("Failed to allocate memory for buffer")

        vkBindBufferMemory(Vulkan.device.logicalDevice, pBuffer[0], pBufferMemory[0], 0)
    }

    fun createVertexBuffer(vertices: Array<Vertex>) {
        val bufferSize = vertices.size * Vertex.BYTE_SIZE
        val pBuffer: LongBuffer = memAllocLong(1)
        val pMemory: LongBuffer = memAllocLong(1)
        createBuffer(bufferSize.toLong(), VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, pBuffer, pMemory)


        // Filling the vertex buffer
        val ppData = memAllocPointer(1)
        vkMapMemory(Vulkan.device.logicalDevice, pMemory[0], 0, bufferSize.toLong(), 0, ppData)
        val data = ppData[0]
        val vertexData = memAlloc(Vertex.BYTE_SIZE * 4)
        Vertex.toByteBuffer(vertices, vertexData)
        vertexData.flip()
        memCopy(memAddress(vertexData), data, bufferSize.toLong())
        memFree(vertexData)
        memFree(ppData)

        vkUnmapMemory(Vulkan.device.logicalDevice, pMemory[0])


        val stagingBuffer = pBuffer[0]
        val stagingBufferMemory = pMemory[0]
        createBuffer(bufferSize.toLong(), VK_BUFFER_USAGE_TRANSFER_DST_BIT or VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, pBuffer, pMemory)

        vertexBuffer = pBuffer[0]
        vertexBufferMemory = pMemory[0]

        copyBuffer(stagingBuffer, vertexBuffer, bufferSize.toLong())

        memFree(pBuffer)
        memFree(pMemory)

        vkDestroyBuffer(Vulkan.device.logicalDevice, stagingBuffer, null)
        vkFreeMemory(Vulkan.device.logicalDevice, stagingBufferMemory, null)
    }

    fun createIndexBuffer() {
        val bufferSize = Vulkan.indices.capacity() * 4
        val pBuffer: LongBuffer = memAllocLong(1)
        val pMemory: LongBuffer = memAllocLong(1)
        createBuffer(bufferSize.toLong(), VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, pBuffer, pMemory)


        // Filling the vertex buffer
        val ppData = memAllocPointer(1)
        vkMapMemory(Vulkan.device.logicalDevice, pMemory[0], 0, bufferSize.toLong(), 0, ppData)
        val data = ppData[0]
        memCopy(memAddress(Vulkan.indices), data, bufferSize.toLong())
        memFree(ppData)

        vkUnmapMemory(Vulkan.device.logicalDevice, pMemory[0])


        // overwrite """pointer buffers""" with the (actual) vertex buffer
        val stagingBuffer = pBuffer[0]
        val stagingBufferMemory = pMemory[0]
        createBuffer(bufferSize.toLong(), VK_BUFFER_USAGE_TRANSFER_DST_BIT or VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT, pBuffer, pMemory)

        indexBuffer = pBuffer[0]
        indexBufferMemory = pMemory[0]

        copyBuffer(stagingBuffer, indexBuffer, bufferSize.toLong())

        memFree(pBuffer)
        memFree(pMemory)

        vkDestroyBuffer(Vulkan.device.logicalDevice, stagingBuffer, null)
        vkFreeMemory(Vulkan.device.logicalDevice, stagingBufferMemory, null)
    }



    private fun copyBuffer(srcBuffer: Long, dstBuffer: Long, size: Long) {
        // We create a temporary command buffer to copy the data
        val allocInfo = VkCommandBufferAllocateInfo.calloc()
        allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
        allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
        allocInfo.commandPool(Vulkan.device.commandPool)
        allocInfo.commandBufferCount(1)

        val pCommandBuffer = memAllocPointer(1)
        vkAllocateCommandBuffers(Vulkan.device.logicalDevice, allocInfo, pCommandBuffer)
        val commandBuffer = VkCommandBuffer(pCommandBuffer[0], Vulkan.device.logicalDevice)

        val beginInfo = VkCommandBufferBeginInfo.calloc()
        beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
        beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)

        vkBeginCommandBuffer(commandBuffer, beginInfo)

        val copyRegion = VkBufferCopy.calloc(1)
        copyRegion.srcOffset(0)
        copyRegion.dstOffset(0)
        copyRegion.size(size)

        vkCmdCopyBuffer(commandBuffer, srcBuffer, dstBuffer, copyRegion)

        vkEndCommandBuffer(commandBuffer)

        // submit directly
        val submitInfo = VkSubmitInfo.calloc(1)
        submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
        val pCommandBuffers = memAllocPointer(1).put(commandBuffer).flip()
        submitInfo.pCommandBuffers(pCommandBuffers)

        vkQueueSubmit(Vulkan.device.graphicsQueue, submitInfo, VK_NULL_HANDLE)
        vkQueueWaitIdle(Vulkan.device.graphicsQueue) // wait for transfer to complete

        vkFreeCommandBuffers(Vulkan.device.logicalDevice, Vulkan.device.commandPool, commandBuffer)


        pCommandBuffers.free()
        beginInfo.free()
        copyRegion.free()
        pCommandBuffer.free()
        allocInfo.free()
    }


    fun cleanup(device: Device) {
        vkDestroyBuffer(device.logicalDevice, vertexBuffer, null)
        vkFreeMemory(device.logicalDevice, vertexBufferMemory, null)
        for (framebuffer in swapchainFramebuffers) {
            vkDestroyFramebuffer(device.logicalDevice, framebuffer, null)
        }

    }
}