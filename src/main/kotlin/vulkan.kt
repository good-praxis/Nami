package com.code.gamerg8.nami

import org.lwjgl.BufferUtils
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugReport.*
import org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR
import org.lwjgl.vulkan.VK10.*


object Vulkan {
    const val WIDTH = 800
    const val HEIGHT = 600
    const val enableValidationLayers = true
    val validationLayers = arrayOf("VK_LAYER_LUNARG_standard_validation")

    private lateinit var window: Window
    private lateinit var vkInstance: VkInstance
    private lateinit var physicalDevice: VkPhysicalDevice

    fun run() {
        this.window = Window(WIDTH, HEIGHT)
        window.getVulkanWindow()

        initVulkan()
        mainLoop()
        cleanup()
    }


    private fun initVulkan() {
        vkInstance = createVulkanInstance()
        setupDebugCallback()
        physicalDevice = pickPhysicalDevice()

    }

    private fun pickPhysicalDevice(): VkPhysicalDevice{
        MemoryStack.stackPush().use {
            val deviceCountBuffer = it.mallocInt(1)
            vkEnumeratePhysicalDevices(vkInstance,deviceCountBuffer,null)
            if (deviceCountBuffer[0] == 0){
                error("failed to find GPUs with Vulkan support!")
            }
            val deviceBuffer = memAllocPointer(deviceCountBuffer[0])
            vkEnumeratePhysicalDevices(vkInstance, deviceCountBuffer, deviceBuffer)

            for(i in 0 until deviceCountBuffer[0]) {
                val handle = deviceBuffer[i]
                val device = VkPhysicalDevice(handle, vkInstance)

                if (isDeviceSuitable(device)) {
                    return device
                }
            }
            error("No suitable GPU found")

        }
    }


    private fun isDeviceSuitable(device: VkPhysicalDevice): Boolean {
        val indices = findQueueFamilies(device)
        if(!indices.isComplete)
            return false
        return true
    }

    private fun findQueueFamilies(device: VkPhysicalDevice): QueueFamilyIndices {
        val indices = QueueFamilyIndices()
        val count = memAllocInt(1)
        vkGetPhysicalDeviceQueueFamilyProperties(device, count, null)
        val families = VkQueueFamilyProperties.calloc(count[0])
        vkGetPhysicalDeviceQueueFamilyProperties(device, count, families)

        var i = 0
        families.forEach {  queueFamily ->
            if(queueFamily.queueCount() > 0 && queueFamily.queueFlags() and VK_QUEUE_GRAPHICS_BIT != 0) {
                indices.graphicsFamily = i
            }

            if(queueFamily.queueCount() > 0) {
                indices.presentFamily = i
            }

            if(indices.isComplete)
                return@forEach
            i++
        }
        return indices
    }

    private fun createVulkanInstance(): VkInstance {
        if(enableValidationLayers)
            this.checkValidationLayerSupport()

        //cleared memory allocation for Vulkans Application Info
        val appInfo = VkApplicationInfo.calloc()
        appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
        appInfo.pApplicationName(memASCII("Hello Triange"))
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

    private fun setupDebugCallback() {
        if(DebugCallback.createDebugCallback(vkInstance) != VK_SUCCESS) {
            error("Couldn't initalized Debug Messenger")
        }
    }

    private fun getRequiredExtensions(): PointerBuffer {
        val extensions = Window.getRequiredExtensions()

        if (enableValidationLayers) {
            extensions.put(memUTF8(VK_EXT_DEBUG_REPORT_EXTENSION_NAME))
            extensions.flip()
        }

        return extensions
    }

    private fun mainLoop() {
        while(!window.shouldWindowClose()) {
            window.pollEvents()
        }
    }

    private fun cleanup() {
        VK10.vkDestroyInstance(vkInstance, null)
        window.destroyAndTerminate()
    }
}