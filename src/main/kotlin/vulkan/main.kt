package com.code.gamerg8.nami.vulkan

import com.code.gamerg8.nami.onKeyDown
import com.code.gamerg8.nami.Util.UINT64_MAX
import com.code.gamerg8.nami.Util.nullptr
import com.code.gamerg8.nami.Window
import com.code.gamerg8.nami.gameLoop
import com.code.gamerg8.nami.onKeyUp
import com.code.gamerg8.nami.timeKeeper.TimeKeeper
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.BufferUtils
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugReport.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import java.nio.IntBuffer
import java.nio.LongBuffer

object Vulkan {
    const val WIDTH = 800
    const val HEIGHT = 600
    const val enableValidationLayers = true
    val validationLayers = arrayOf("VK_LAYER_LUNARG_standard_validation")
    val deviceExtensions = arrayOf(VK_KHR_SWAPCHAIN_EXTENSION_NAME)
    const val MaxFramesInFlight = 2
    var startTime: Long = nullptr

    var tempRotation: Int = 0 //TODO: refactor
    var tempAngle: Double = 0.0

    val vertexArray: Array<Vertex> = arrayOf(
        Vertex(Vector2f(-0.5f, -0.5f), Vector3f(1.0f, 0.0f, 0.0f)),
        Vertex(Vector2f(0.5f, -0.5f), Vector3f(0.0f, 1.0f, 0.0f)),
        Vertex(Vector2f(0.5f, 0.5f), Vector3f(0.0f, 0.0f, 1.0f)),
        Vertex(Vector2f(-0.5f, 0.5f), Vector3f(1.0f, 1.0f, 1.0f))
    )

    lateinit var window: Window
    lateinit var vkInstance: VkInstance
    lateinit var device: Device
    lateinit var pipeline: GraphicsPipeline
    lateinit var buffers: Buffers
    lateinit var inFlightFences: Array<Long>
    lateinit var descriptorSets: Array<Long>

    val indices = memAllocInt(6)
        .put(0).put(1).put(2).put(2).put(3).put(0)
        .flip() as IntBuffer

    fun run(gameLoop: Unit) {
        initVulkan()
        mainLoop(gameLoop)
        cleanup()
    }


    fun initVulkan() {
        this.window = Window(WIDTH, HEIGHT)
        vkInstance = createVulkanInstance()

        if(enableValidationLayers) DebugCallback.createDebugCallback() // TODO: REFACTOR TO CLASS

        window.getVulkanSurface()

        device = Device()
        device.pickAndBindDevice()
        device.setupSwapchain()

        pipeline = GraphicsPipeline()
        pipeline.createRenderPass()
        pipeline.createDescriptorSetLayout()
        pipeline.createGraphicsPipeline()


        buffers = Buffers()
        buffers.createFramebuffers()
        device.createCommandPool()

        buffers.createVertexBuffer(vertexArray)
        buffers.createIndexBuffer()
        buffers.createUniformBuffers()
        device.createDescriptorPool()
        createDescriptorSets()
        buffers.createCommandBuffers()
        pipeline.createSyncObjects()

        window.inputManager.onKeyDown += ::onKeyDown //TODO: Please find another place for this later
        window.inputManager.onKeyUp += ::onKeyUp
    }


    private fun createVulkanInstance(): VkInstance {
        if(enableValidationLayers)
            this.checkValidationLayerSupport()

        //cleared memory allocation for Vulkans Application Info
        val appInfo = VkApplicationInfo.calloc()
        appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
        appInfo.pApplicationName(memASCII("Hello Triange")) //TODO: FIX TYPO
        appInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0))
        appInfo.pEngineName(memASCII("Nami"))
        appInfo.engineVersion(VK_MAKE_VERSION(1, 0, 0))
        appInfo.apiVersion(VK_API_VERSION_1_0)

        val instanceInfo = VkInstanceCreateInfo.calloc()
        instanceInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
        instanceInfo.pApplicationInfo(appInfo)

        val extensions = getRequiredExtensions()
        instanceInfo.ppEnabledExtensionNames(extensions)

        if (enableValidationLayers) {
            val layers = BufferUtils.createPointerBuffer(validationLayers.size)
            for (layer in validationLayers) {
                layers.put(memUTF8(layer))
            }
            layers.flip()
            instanceInfo.ppEnabledLayerNames(layers)

        }
        val handle = MemoryStack.stackPush().use {
            val instancePointer = it.mallocPointer(1)
            if(vkCreateInstance(instanceInfo, null, instancePointer) != VK_SUCCESS) {
                error("Failed to create instance")
            }
            instancePointer[0]
        }
        return VkInstance(handle, instanceInfo)
    }

    private fun checkValidationLayerSupport() {
        MemoryStack.stackPush().use {
            val countBuffer = it.mallocInt(1)
            vkEnumerateInstanceLayerProperties(countBuffer, null)
            val layers = VkLayerProperties.calloc(countBuffer[0])
            vkEnumerateInstanceLayerProperties(countBuffer, layers)

            for(layerName in validationLayers) {
                var found = false

                layers.forEach { layer ->
                    if(layer.layerNameString() == layerName) {
                        found = true
                        return@forEach
                    }
                }

                if (!found) {
                    error("Missing validation layer '$layerName'")
                }
            }
        }

        println("Found all validation layers")
    }


    private fun getRequiredExtensions(): PointerBuffer {
        val glfwExtensions = Window.getRequiredExtensions()!!
        if(enableValidationLayers) {
            val extensions = BufferUtils.createPointerBuffer(glfwExtensions.capacity() + 1)
            extensions.put(glfwExtensions)
            extensions.put(memUTF8(VK_EXT_DEBUG_REPORT_EXTENSION_NAME))
            extensions.flip()
            return extensions
        }
        return glfwExtensions
    }


    private fun mainLoop(gameLoop: Unit) {
        var currentFrame = 0

        while(!window.shouldWindowClose()) {
            window.pollEvents()
            gameLoop()
            drawFrame(currentFrame)

            currentFrame = (currentFrame+1) % MaxFramesInFlight
        }

        vkDeviceWaitIdle(device.logicalDevice)
    }

    private fun createDescriptorSets() {
        val layouts = memAllocLong(device.swapchainImages.size)
        repeat(device.swapchainImages.size) {
            layouts.put(pipeline.descriptorSetLayout)
        }
        layouts.flip()
        val allocInfo = VkDescriptorSetAllocateInfo.calloc()
        allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
        allocInfo.descriptorPool(device.descriptorPool)
        allocInfo.pSetLayouts(layouts)

        val pSets = memAllocLong(device.swapchainImages.size)
        if(vkAllocateDescriptorSets(device.logicalDevice, allocInfo, pSets) != VK_SUCCESS) {
            error("Failed to allocate descriptor sets")
        }
        descriptorSets = Array<Long>(device.swapchainImages.size) { i -> pSets[i] }

        memFree(pSets)

        // populate descriptors
        val descriptorWrites = VkWriteDescriptorSet.calloc(descriptorSets.size)
        for(i in 0 until descriptorSets.size) {
            val bufferInfo = VkDescriptorBufferInfo.calloc(1)
            bufferInfo.buffer(buffers.uniformBuffers[i])
            bufferInfo.offset(0)
            bufferInfo.range(UniformBufferObject.SizeOfUniformBufferObject.toLong())

            val descriptorWrite = descriptorWrites[i]
            descriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
            descriptorWrite.dstSet(descriptorSets[i])
            descriptorWrite.dstBinding(0)
            descriptorWrite.dstArrayElement(0)

            descriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)

            descriptorWrite.pBufferInfo(bufferInfo)
        }

        vkUpdateDescriptorSets(device.logicalDevice, descriptorWrites, null)
    }

    private fun drawFrame(currentFrame: Int) {
        vkWaitForFences(device.logicalDevice, inFlightFences[currentFrame], true, UINT64_MAX)
        vkResetFences(device.logicalDevice, inFlightFences[currentFrame])
        MemoryStack.stackPush().use { mem ->
            val imageIndex = mem.mallocInt(1)
            val err = vkAcquireNextImageKHR(device.logicalDevice, device.swapchain, UINT64_MAX, pipeline.imageAvailableSemaphores[currentFrame], VK_NULL_HANDLE, imageIndex)
            when(err) {
                VK_SUCCESS, VK_SUBOPTIMAL_KHR -> Unit
                VK_ERROR_OUT_OF_DATE_KHR -> { device.recreateSwapChain(); return@use }
                else -> error("Failed to acquire swap chain images $err")
            }

            imageIndex.rewind()
            updateUniformBuffer(imageIndex[0])

            val submitInfo = VkSubmitInfo.calloc()
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)

            val waitSemaphores = BufferUtils.createLongBuffer(1).put(pipeline.imageAvailableSemaphores[currentFrame]).flip() as LongBuffer
            val waitStages = BufferUtils.createIntBuffer(1).put(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT).flip() as IntBuffer
            submitInfo.pWaitDstStageMask(waitStages)
            submitInfo.pWaitSemaphores(waitSemaphores)
            submitInfo.waitSemaphoreCount(1)

            val commandBuffer = buffers.commandBuffers[imageIndex[0]]
            val pCommandBuffers = BufferUtils.createPointerBuffer(1)
                .put(commandBuffer)
                .flip()
            submitInfo.pCommandBuffers(pCommandBuffers)

            val signalSemaphores = BufferUtils.createLongBuffer(1).put(pipeline.renderFinishedSemaphores[currentFrame]).flip() as LongBuffer
            submitInfo.pSignalSemaphores(signalSemaphores)

            vkQueueSubmit(device.graphicsQueue, submitInfo, inFlightFences[currentFrame])

            val presentInfo = VkPresentInfoKHR.calloc()
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
            presentInfo.pWaitSemaphores(signalSemaphores)
            presentInfo.pResults(null)
            presentInfo.swapchainCount(1)

            val swapchains = BufferUtils.createLongBuffer(1).put(device.swapchain).flip() as LongBuffer
            presentInfo.pSwapchains(swapchains)
            presentInfo.pImageIndices(imageIndex)

            val err2 = vkQueuePresentKHR(device.presentQueue, presentInfo)
            when(err2) {
                VK_SUCCESS -> Unit
                VK_SUBOPTIMAL_KHR, VK_ERROR_OUT_OF_DATE_KHR -> device.recreateSwapChain()
                else -> error("Failed to present swap chain images $err2")
            }

            presentInfo.pResults(null)

            vkQueueWaitIdle(device.presentQueue)
        }
    }

    fun updateUniformBuffer(currentFrame: Int) {
        if(startTime  == nullptr) startTime = System.currentTimeMillis()
        val ubo = UniformBufferObject()
        val upAxis = Vector3f(0f, 0f, 1f)

        val deltaTime = TimeKeeper.getDeltaSinceLastCall()

        tempAngle += (deltaTime / 1000f * Math.PI/2f) * tempRotation
        val angle = tempAngle
        ubo.model.identity().rotate(angle.toFloat(), upAxis)

        val eyePos = Vector3f(2f)
        val centerPos = Vector3f(0f)

        ubo.view.setLookAt(eyePos, centerPos, upAxis)

        ubo.proj.setPerspective(45f * Math.PI.toFloat() / 180f,
            device.swapchainExtent.width().toFloat() / device.swapchainExtent.height().toFloat(),
            0.1f, 10f)
        ubo.proj.m11(ubo.proj.m11()* (-1f)) // invert Y Axis

        val ppData = memAllocPointer(1)
        vkMapMemory(device.logicalDevice, buffers.uniformBuffersMemory[currentFrame], 0, UniformBufferObject.SizeOfUniformBufferObject.toLong(), 0, ppData)
        val data = ppData[0]

        val uboData = memAlloc(UniformBufferObject.SizeOfUniformBufferObject)
        ubo.putIn(uboData)
        uboData.flip()
        memCopy(memAddress(uboData), data, UniformBufferObject.SizeOfUniformBufferObject.toLong())

        vkUnmapMemory(device.logicalDevice, buffers.uniformBuffersMemory[currentFrame])

        memFree(uboData)
        memFree(ppData)
    }

    private fun cleanup() {
        buffers.cleanup(device)
        pipeline.cleanup(device)
        device.cleanup()
        window.destroySurface()

        if(enableValidationLayers) DebugCallback.cleanup()

        vkDestroyInstance(vkInstance, null)
        window.destroyAndTerminate()
    }
}