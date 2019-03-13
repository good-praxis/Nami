package com.code.gamerg8.nami.vulkan

import com.code.gamerg8.nami.Util.nullptr
import com.code.gamerg8.nami.Vulkan
import org.lwjgl.BufferUtils
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import java.nio.ByteBuffer

class Device {
    lateinit var logicalDevice: VkDevice
    lateinit var physicalDevice: VkPhysicalDevice
    lateinit var queueFamilyIndices: QueueFamilyIndices
    lateinit var graphicsQueue: VkQueue
    lateinit var presentQueue: VkQueue
    var swapchain: Long = nullptr
    lateinit var swapchainDetails: SwapChainSupportDetails
    lateinit var swapchainFormat: VkSurfaceFormatKHR
    var swapchainImageFormat: Int = 0
    var swapchainPresentMode: Int = 0
    lateinit var swapchainExtent: VkExtent2D
    lateinit var swapchainImageViews: Array<Long>
    lateinit var swapchainImages: Array<Long>
    var commandPool: Long = nullptr
    var descriptorPool: Long = nullptr

    fun pickAndBindDevice() {
        pickPhysicalDevice()
        setQueueFamilyIndices()
        createLogicalDevice()
        createQueues()
    }

    fun setupSwapchain() {
        this.swapchainDetails = querySwapChainSupport(this.physicalDevice)

        chooseSwapSurfaceFormat()
        chooseSwapImageFormat()
        chooseSwapPresentMode()
        chooseSwapExtent()
        createSwapChain()
        createImageViews()
    }

    fun prepareRecreatedSwapchain() {
        Vulkan.pipeline.createRenderPass()
        Vulkan.pipeline.createDescriptorSetLayout()
        Vulkan.pipeline.createGraphicsPipeline()
        Vulkan.buffers.createFramebuffers()
        Vulkan.buffers.createCommandBuffers()
    }

    fun recreateSwapChain() {
        vkDeviceWaitIdle(logicalDevice)
        cleanupSwapchain()

        setupSwapchain()
        prepareRecreatedSwapchain()
    }

    fun createCommandPool() {
        val queueFamilyIndices = findQueueFamilies(physicalDevice)
        val poolInfo = VkCommandPoolCreateInfo.calloc()
        poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
        poolInfo.queueFamilyIndex(queueFamilyIndices.graphicsFamily)

        commandPool = MemoryStack.stackPush().use {
            val pPool = it.mallocLong(1)

            if(vkCreateCommandPool(logicalDevice, poolInfo, null, pPool) != VK_SUCCESS) {
                error("Failed to create command pool")
            }
            pPool[0]
        }
    }

    fun findMemoryType(typeFiler: Int, properties: Int): Int {
        val memProperties = VkPhysicalDeviceMemoryProperties.calloc()
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProperties)

        for (i in 0 until memProperties.memoryTypeCount()) {
            if (typeFiler and (1 shl i) != 0 && memProperties.memoryTypes(i).propertyFlags() and properties == properties) {
                return i
            }
        }
        error("failed to find suitable memory type!")
    }

    fun createDescriptorPool() {
        val poolSize = VkDescriptorPoolSize.calloc(1)
        poolSize.descriptorCount(swapchainImages.size)
        poolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)

        val poolInfo = VkDescriptorPoolCreateInfo.calloc()
        poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
        poolInfo.pPoolSizes(poolSize)
        poolInfo.maxSets(swapchainImages.size)

        val pPool = memAllocLong(1)

        if(vkCreateDescriptorPool(logicalDevice, poolInfo, null, pPool) != VK_SUCCESS) {
            error("Could not create descriptor pool")
        }

        descriptorPool = pPool[0]

        memFree(pPool)
    }

    fun cleanup() {
        vkDestroyDescriptorPool(logicalDevice, descriptorPool, null)

        vkDestroyCommandPool(logicalDevice, commandPool, null)
        for(imageView in swapchainImageViews) {
            vkDestroyImageView(logicalDevice, imageView, null)
        }
        vkDestroySwapchainKHR(logicalDevice, swapchain, null)
        vkDestroyDevice(this.logicalDevice, null)
    }

    fun cleanupSwapchain() {
        vkDestroyDescriptorSetLayout(logicalDevice, Vulkan.pipeline.descriptorSetLayout, null)
        for(framebuffer in Vulkan.buffers.swapchainFramebuffers) {
            vkDestroyFramebuffer(logicalDevice, framebuffer, null)
        }

        val commandBufferPointers = memAllocPointer(Vulkan.buffers.commandBuffers.size)
        Vulkan.buffers.commandBuffers.forEach { commandBufferPointers.put(it) }
        commandBufferPointers.flip()
        vkFreeCommandBuffers(logicalDevice, commandPool, commandBufferPointers)
        memFree(commandBufferPointers)

        vkDestroyPipeline(logicalDevice, Vulkan.pipeline.pipeline, null)
        vkDestroyPipelineLayout(logicalDevice, Vulkan.pipeline.pipelineLayout, null)
        vkDestroyRenderPass(logicalDevice, Vulkan.pipeline.renderPass, null)
        for(imageView in swapchainImageViews) {
            vkDestroyImageView(logicalDevice, imageView, null)
        }
        vkDestroySwapchainKHR(logicalDevice, swapchain, null)
    }

    private fun pickPhysicalDevice(){
        MemoryStack.stackPush().use {
            val instance = Vulkan.vkInstance
            val deviceCountBuffer = it.mallocInt(1)
            vkEnumeratePhysicalDevices(instance, deviceCountBuffer, null)
            if (deviceCountBuffer[0] == 0){
                error("failed to find GPUs with Vulkan support!")
            }
            val deviceBuffer = MemoryUtil.memAllocPointer(deviceCountBuffer[0])
            vkEnumeratePhysicalDevices(instance, deviceCountBuffer, deviceBuffer)

            for(i in 0 until deviceCountBuffer[0]) {
                val handle = deviceBuffer[i]
                val device = VkPhysicalDevice(handle, instance)

                if (isDeviceSuitable(device)) {
                    this.physicalDevice = device
                    return
                }
            }
            error("No suitable GPU found")

        }
    }

    private fun createLogicalDevice() {
        this.logicalDevice = MemoryStack.stackPush().use {
            val queueCreateInfo = VkDeviceQueueCreateInfo.calloc(2)
            val uniqueQueueFamilies = arrayOf(queueFamilyIndices.graphicsFamily, queueFamilyIndices.presentFamily)
            for ((index, queueFamily) in uniqueQueueFamilies.withIndex()) {
                val info = queueCreateInfo[index]
                info.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                info.queueFamilyIndex(queueFamily)
                info.pQueuePriorities(MemoryStack.stackFloats(1f))
            }

            val createInfo = VkDeviceCreateInfo.calloc()
            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
            createInfo.pQueueCreateInfos(queueCreateInfo)

            val deviceFeatures = VkPhysicalDeviceFeatures.calloc()
            createInfo.pEnabledFeatures(deviceFeatures)
            val pExtensionNames = it.mallocPointer(Vulkan.deviceExtensions.size)
            for (extension in Vulkan.deviceExtensions) {
                pExtensionNames.put(memUTF8(extension))
            }
            pExtensionNames.flip()
            createInfo.ppEnabledExtensionNames(pExtensionNames)

            val handle = MemoryStack.stackPush().use {
                if (Vulkan.enableValidationLayers) {

                    val layers = BufferUtils.createPointerBuffer(Vulkan.validationLayers.size)
                    for (layer in Vulkan.validationLayers) {
                        layers.put(memUTF8(layer))
                    }
                    layers.flip()
                    createInfo.ppEnabledLayerNames(layers)
                }
                val pDevice = it.mallocPointer(1)
                if (vkCreateDevice(physicalDevice, createInfo, null, pDevice) != VK_SUCCESS)
                    error("Failed to create logical device")
                pDevice[0]
            }
            VkDevice(handle, physicalDevice, createInfo)
        }
    }

    private fun createQueues() {
        this.graphicsQueue = createQueue(this.queueFamilyIndices.graphicsFamily)
        this.presentQueue = createQueue(this.queueFamilyIndices.presentFamily)
    }

    private fun createQueue(index: Int): VkQueue {
        val handle = MemoryStack.stackPush().use {
            val pQueue = it.mallocPointer(1)
            vkGetDeviceQueue(logicalDevice, index, 0, pQueue)
            pQueue[0]
        }

        return VkQueue(handle, logicalDevice)
    }

    private fun setQueueFamilyIndices() {
        this.queueFamilyIndices = findQueueFamilies(this.physicalDevice)
    }


    private fun isDeviceSuitable(device: VkPhysicalDevice): Boolean {
        val indices = findQueueFamilies(device)
        if(!indices.isComplete)
            return false
        if(!checkDeviceExtensionSupport(device))
            return false
        val swapChainSupport = querySwapChainSupport(device)
        if((swapChainSupport.formats.capacity() == 0) || swapChainSupport.presentModes.isEmpty())
            return false

        return true
    }

    private fun checkDeviceExtensionSupport(device: VkPhysicalDevice): Boolean {
        return MemoryStack.stackPush().use {
            val count = it.mallocInt(1)
            vkEnumerateDeviceExtensionProperties(device, null as? ByteBuffer, count, null)

            val availableExtensions = VkExtensionProperties.callocStack(count[0])
            vkEnumerateDeviceExtensionProperties(device, null as? ByteBuffer, count, availableExtensions)

            val requiredExtensions = mutableListOf(*Vulkan.deviceExtensions)
            availableExtensions.forEach {
                requiredExtensions -= it.extensionNameString()
            }

            if(requiredExtensions.isNotEmpty()) {
                println("Missing extensions:")
                for(required in requiredExtensions) {
                    println("\t- $required")
                }
            }
            requiredExtensions.isEmpty()
        }
    }

    private fun querySwapChainSupport(device: VkPhysicalDevice): SwapChainSupportDetails { // TODO: ADD ERROR?
        val capabilities = VkSurfaceCapabilitiesKHR.calloc()
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, Vulkan.window.surface, capabilities)

        return MemoryStack.stackPush().use {
            val formatCount = it.mallocInt(1)
            vkGetPhysicalDeviceSurfaceFormatsKHR(device, Vulkan.window.surface, formatCount, null)

            val pFormats = VkSurfaceFormatKHR.calloc(formatCount[0])
            vkGetPhysicalDeviceSurfaceFormatsKHR(device, Vulkan.window.surface, formatCount, pFormats)

            val presentModeCount = it.mallocInt(1)
            vkGetPhysicalDeviceSurfacePresentModesKHR(device, Vulkan.window.surface, presentModeCount, null)

            val modes = it.mallocInt(presentModeCount[0])
            vkGetPhysicalDeviceSurfacePresentModesKHR(device, Vulkan.window.surface, presentModeCount, modes)

            val modeArray = Array<Int>(presentModeCount[0]) { i -> modes[i] }

            SwapChainSupportDetails(capabilities, pFormats, modeArray)
        }
    }


    private fun findQueueFamilies(device: VkPhysicalDevice): QueueFamilyIndices {
        val indices = QueueFamilyIndices()
        val count = MemoryUtil.memAllocInt(1)
        vkGetPhysicalDeviceQueueFamilyProperties(device, count, null)
        val families = VkQueueFamilyProperties.calloc(count[0])
        vkGetPhysicalDeviceQueueFamilyProperties(device, count, families)

        var i = 0
        families.forEach {  queueFamily ->
            if(queueFamily.queueCount() > 0 && queueFamily.queueFlags() and VK_QUEUE_GRAPHICS_BIT != 0) {
                indices.graphicsFamily = i
            }

            val presentSupport = MemoryStack.stackPush().use {
                val pSupport = it.mallocInt(1)
                vkGetPhysicalDeviceSurfaceSupportKHR(device, i, Vulkan.window.surface, pSupport)
                pSupport[0] == VK_TRUE
            }

            if(queueFamily.queueCount() > 0 && presentSupport) {
                indices.presentFamily = i
            }

            if(indices.isComplete)
                return@forEach // TODO: RESEARCH WHY THIS WORKS
            i++
        }
        return indices
    }

    private fun chooseSwapSurfaceFormat()  {
        if(swapchainDetails.formats.capacity() == 1 && swapchainDetails.formats[0].format() == VK_FORMAT_UNDEFINED) { // no preferred format
            val buffer = BufferUtils.createByteBuffer(2 * 4)
            buffer.putInt(VK_FORMAT_B8G8R8A8_UNORM)
            buffer.putInt(VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
            buffer.flip()
            this.swapchainFormat = VkSurfaceFormatKHR(buffer)
        }

        swapchainDetails.formats.forEach { format ->
            if(format.format() == VK_FORMAT_B8G8R8A8_UNORM && format.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                this.swapchainFormat = format
            }
        }
    }

    private fun chooseSwapImageFormat() {
        this.swapchainImageFormat = this.swapchainFormat.format()
    }

    private fun chooseSwapPresentMode() {
        var bestMode = VK_PRESENT_MODE_FIFO_KHR

        for(mode in this.swapchainDetails.presentModes) {
            if(mode == VK_PRESENT_MODE_MAILBOX_KHR) {
                this.swapchainPresentMode = mode
                return
            }
            else if(mode == VK_PRESENT_MODE_IMMEDIATE_KHR)
                bestMode = mode
        }

        this.swapchainPresentMode = bestMode
    }

    private fun chooseSwapExtent() {
        if((this.swapchainDetails.capabilities.currentExtent().width().toLong() and 0xFFFFFFFF) != (Integer.MAX_VALUE.toLong() and 0xFFFFFFFF)) { // TODO: LINE TOO LONG
            this.swapchainExtent = this.swapchainDetails.capabilities.currentExtent()
            return
        }

        val buffer = BufferUtils.createByteBuffer(2*4)
        buffer.putInt(Vulkan.WIDTH)
        buffer.putInt(Vulkan.HEIGHT)
        buffer.flip()
        val actualExtent = VkExtent2D(buffer)

        val w = maxOf(
            this.swapchainDetails.capabilities.minImageExtent().width(),
            minOf(
                this.swapchainDetails.capabilities.maxImageExtent().width(),
                actualExtent.width()
            )
        )
        val h = maxOf(this.swapchainDetails.capabilities.minImageExtent().height(), minOf(this.swapchainDetails.capabilities.maxImageExtent().height(), actualExtent.height()))
        actualExtent.width(w)
        actualExtent.height(h)

        this.swapchainExtent = actualExtent
    }

    private fun createSwapChain() {
        var imageCount = this.swapchainDetails.capabilities.minImageCount() +1
        if(this.swapchainDetails.capabilities.maxImageCount() > 0 && imageCount > this.swapchainDetails.capabilities.maxImageCount()) { // check if upper limit
            imageCount = this.swapchainDetails.capabilities.maxImageCount()
        }

        val createInfo = VkSwapchainCreateInfoKHR.calloc()
        createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
        createInfo.surface(Vulkan.window.surface)
        createInfo.minImageCount(imageCount)
        createInfo.imageFormat(this.swapchainImageFormat)
        createInfo.imageColorSpace(this.swapchainFormat.colorSpace())
        createInfo.imageExtent(this.swapchainExtent)
        createInfo.imageArrayLayers(1) // not 1 only for 3D stereoscopic apps
        createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)

        val indices = findQueueFamilies(physicalDevice)
        if(indices.graphicsFamily != indices.presentFamily) {
            createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
            val familyIndices = BufferUtils.createIntBuffer(2).put(indices.graphicsFamily).put(indices.presentFamily)
            familyIndices.flip()
            createInfo.pQueueFamilyIndices(familyIndices)
        } else {
            createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
        }

        createInfo.preTransform(this.swapchainDetails.capabilities.currentTransform())

        createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)

        createInfo.presentMode(this.swapchainPresentMode)
        createInfo.clipped(true)
        createInfo.oldSwapchain(nullptr)

        this.swapchain = MemoryStack.stackPush().use {
            val pSwapChain = it.mallocLong(1)
            if(vkCreateSwapchainKHR(logicalDevice, createInfo, null, pSwapChain) != VK_SUCCESS) {
                error("Failed to create swap chain")
            }

            pSwapChain[0]
        }

        this.swapchainImages = MemoryStack.stackPush().use {
            val pImageCount = it.mallocInt(1)
            vkGetSwapchainImagesKHR(logicalDevice, swapchain, pImageCount, null)

            val images = it.mallocLong(pImageCount[0])
            vkGetSwapchainImagesKHR(logicalDevice, swapchain, pImageCount, images)

            Array<Long>(pImageCount[0]) { i -> images[i] }
        }
    }

    private fun createImageViews() {
        val result = Array<Long>(swapchainImages.size) { -1 }
        swapchainImages.forEachIndexed { index, image ->
            val createInfo = VkImageViewCreateInfo.calloc()
            createInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
            createInfo.image(image)
            createInfo.viewType(VK_IMAGE_VIEW_TYPE_2D)
            createInfo.format(swapchainImageFormat)

            createInfo.components()
                .r(VK_COMPONENT_SWIZZLE_IDENTITY)
                .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                .a(VK_COMPONENT_SWIZZLE_IDENTITY)

            createInfo.subresourceRange()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT) // color target
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1)

            val handle = MemoryStack.stackPush().use {
                val pView = it.mallocLong(1)
                vkCreateImageView(logicalDevice, createInfo, null, pView)
                pView[0]
            }
            result[index] = handle
        }

        this.swapchainImageViews = result
    }
}




data class QueueFamilyIndices(var graphicsFamily: Int=-1, var presentFamily: Int=-1) {
    val isComplete get()= graphicsFamily >= 0 && presentFamily >= 0
}

data class SwapChainSupportDetails(var capabilities: VkSurfaceCapabilitiesKHR, val formats: VkSurfaceFormatKHR.Buffer, val presentModes: Array<Int>) {
}