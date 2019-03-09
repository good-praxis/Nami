package com.code.gamerg8.nami.vulkan

import com.code.gamerg8.nami.Vulkan
import org.lwjgl.BufferUtils
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.VK10.*
import java.nio.ByteBuffer

class Device {
    lateinit var logicalDevice: VkDevice
    lateinit var physicalDevice: VkPhysicalDevice
    lateinit var queueFamilyIndices: QueueFamilyIndices


    fun pickAndBindDevice() {
        pickPhysicalDevice()
        setQueueFamilyIndices()
        createLogicalDevice()
    }

    fun cleanup() {
        vkDestroyDevice(this.logicalDevice, null)
    }

    private fun pickPhysicalDevice(){
        MemoryStack.stackPush().use {
            val instance = Vulkan.getVkInstance()
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

    private fun querySwapChainSupport(device: VkPhysicalDevice): SwapChainSupportDetails {
        val capabilities = VkSurfaceCapabilitiesKHR.calloc()
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, Vulkan.getWindow().surface, capabilities)

        return MemoryStack.stackPush().use {
            val formatCount = it.mallocInt(1)
            vkGetPhysicalDeviceSurfaceFormatsKHR(device, Vulkan.getWindow().surface, formatCount, null)

            val pFormats = VkSurfaceFormatKHR.calloc(formatCount[0])
            vkGetPhysicalDeviceSurfaceFormatsKHR(device, Vulkan.getWindow().surface, formatCount, pFormats)

            val presentModeCount = it.mallocInt(1)
            vkGetPhysicalDeviceSurfacePresentModesKHR(device, Vulkan.getWindow().surface, presentModeCount, null)

            val modes = it.mallocInt(presentModeCount[0])
            vkGetPhysicalDeviceSurfacePresentModesKHR(device, Vulkan.getWindow().surface, presentModeCount, modes)

            val modeArray = Array<Int>(presentModeCount[0]) { i -> modes[i] }

            SwapChainSupportDetails(capabilities, pFormats, modeArray)
        }
    }

    private fun findQueueFamilies(device: VkPhysicalDevice): QueueFamilyIndices {
        val indices = QueueFamilyIndices()
        val count = MemoryUtil.memAllocInt(1)
        VK10.vkGetPhysicalDeviceQueueFamilyProperties(device, count, null)
        val families = VkQueueFamilyProperties.calloc(count[0])
        VK10.vkGetPhysicalDeviceQueueFamilyProperties(device, count, families)

        var i = 0
        families.forEach {  queueFamily ->
            if(queueFamily.queueCount() > 0 && queueFamily.queueFlags() and VK10.VK_QUEUE_GRAPHICS_BIT != 0) {
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
}


data class QueueFamilyIndices(var graphicsFamily: Int=-1, var presentFamily: Int=-1) {
    val isComplete get()= graphicsFamily >= 0 && presentFamily >= 0
}

data class SwapChainSupportDetails(var capabilities: VkSurfaceCapabilitiesKHR, val formats: VkSurfaceFormatKHR.Buffer, val presentModes: Array<Int>) {
}