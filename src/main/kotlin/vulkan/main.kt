package com.code.gamerg8.nami

import com.code.gamerg8.nami.Util.UINT64_MAX
import com.code.gamerg8.nami.vulkan.*
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


    lateinit var window: Window
    lateinit var vkInstance: VkInstance
    lateinit var device: Device
    lateinit var pipeline: GraphicsPipeline
    lateinit var buffers: Buffers
    lateinit var inFlightFences: Array<Long>

    val indices = memAllocInt(6)
        .put(0).put(1).put(2).put(2).put(3).put(0)
        .flip() as IntBuffer

    fun run() {
        this.window = Window(WIDTH, HEIGHT)

        initVulkan()
        mainLoop()
        cleanup()
    }


    private fun initVulkan() {
        vkInstance = createVulkanInstance()

        if(enableValidationLayers) DebugCallback.createDebugCallback() // TODO: REFACTOR TO CLASS

        window.getVulkanSurface()

        device = Device()
        device.pickAndBindDevice()
        device.setupSwapchain()

        pipeline = GraphicsPipeline()
        pipeline.createRenderPass()
        pipeline.createGraphicsPipeline()

        buffers = Buffers()
        buffers.createFramebuffers()
        device.createCommandPool()

        // TODO: THIS IS A PLACEHOLDER
        val vertexArray: Array<Vertex> = arrayOf(
            Vertex(Vector2f(-0.5f, -0.5f), Vector3f(1.0f, 0.0f, 0.0f)),
            Vertex(Vector2f(0.5f, -0.5f), Vector3f(0.0f, 1.0f, 0.0f)),
            Vertex(Vector2f(0.5f, 0.5f), Vector3f(0.0f, 0.0f, 1.0f)),
            Vertex(Vector2f(-0.5f, 0.5f), Vector3f(1.0f, 1.0f, 1.0f))
        )

        buffers.createVertexBuffer(vertexArray)
        buffers.createIndexBuffer()
        buffers.createCommandBuffers()
        pipeline.createSyncObjects()



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


    private fun mainLoop() {
        var currentFrame = 0

        while(!window.shouldWindowClose()) {
            window.pollEvents()
            drawFrame(currentFrame)

            currentFrame = (currentFrame+1) % MaxFramesInFlight
        }

        vkDeviceWaitIdle(device.logicalDevice)
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