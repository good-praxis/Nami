package com.code.gamerg8.nami.vulkan

import com.code.gamerg8.nami.Util
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.*

class Buffers {
    lateinit var swapchainFramebuffers: Array<Long>
    lateinit var commandBuffers: Array<VkCommandBuffer>

    fun createFramebuffers(device: Device, pipeline: GraphicsPipeline) {
        swapchainFramebuffers = Array(device.swapchainImageViews.size) { Util.nullptr }
        for((index, view) in device.swapchainImageViews.withIndex()) {
            val attachments = memAllocLong(1)
            attachments.put(view).flip()

            val framebufferInfo = VkFramebufferCreateInfo.calloc()
            framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
            framebufferInfo.renderPass(pipeline.renderPass)
            framebufferInfo.pAttachments(attachments)
            framebufferInfo.width(device.swapchainExtent.width())
            framebufferInfo.height(device.swapchainExtent.height())
            framebufferInfo.layers(1)

            swapchainFramebuffers[index] = MemoryStack.stackPush().use {
                val pFramebuffer = it.mallocLong(1)
                if(vkCreateFramebuffer(device.logicalDevice, framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                    error("Failed to create framebuffer")
                }
                pFramebuffer[0]
            }

        }
    }

    fun createCommandBuffers(device: Device, pipeline: GraphicsPipeline) { // TODO: RESEARCH COMMAND BUFFERS
        val allocInfo = VkCommandBufferAllocateInfo.calloc()
        allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
        allocInfo.commandPool(device.commandPool)
        allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
        allocInfo.commandBufferCount(swapchainFramebuffers.size)

        MemoryStack.stackPush().use {
            val commandBufferPointers = it.mallocPointer(swapchainFramebuffers.size)
            if(vkAllocateCommandBuffers(device.logicalDevice, allocInfo, commandBufferPointers) != VK_SUCCESS) {
                error("Failed to allocate command buffers")
            }

            commandBuffers = Array(swapchainFramebuffers.size) { i ->
                VkCommandBuffer(commandBufferPointers[i], device.logicalDevice)
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
            renderPassInfo.renderPass(pipeline.renderPass)
            renderPassInfo.framebuffer(swapchainFramebuffers[index])
            val renderArea = VkRect2D.calloc()
            renderArea.offset(VkOffset2D.calloc().set(0,0))
            renderArea.extent(device.swapchainExtent)
            renderPassInfo.renderArea(renderArea)

            val clearValues = VkClearValue.calloc(1)
            clearValues.color().float32(0, 0f).float32(1, 1f).float32(2, 1f).float32(3, 1f)
            renderPassInfo.pClearValues(clearValues)

            vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE)

            // drawing commands
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.pipeline)
            vkCmdDraw(commandBuffer, 3, 1, 0, 0) // draw 3 vertices (1 instance)

            vkCmdEndRenderPass(commandBuffer)

            if(vkEndCommandBuffer(commandBuffer) != VK_SUCCESS)
                error("Failed to record command buffer")
        }
    }

    fun cleanup(device: Device) {
        for (framebuffer in swapchainFramebuffers) {
            vkDestroyFramebuffer(device.logicalDevice, framebuffer, null)
        }
    }
}