package com.code.gamerg8.nami

import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWVulkan.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.*

object HelloTriangle {
    val nullptr = NULL
    private val WIDTH = 800
    private val HEIGHT = 600
    private var windowPointer: Long = -1
    private const val enableValidationLayers = true
    private lateinit var vkInstance: VkInstance


    fun run() {
        initWindow()
        initVulkan()
        mainLoop()
        cleanup()
    }


    private fun initWindow() {
        glfwInit()

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)

        windowPointer = glfwCreateWindow(WIDTH, HEIGHT, "Vulkan", nullptr, nullptr)
    }


    private fun initVulkan() {
        vkInstance = createVulkanInstance()

    }

    private fun createVulkanInstance(): VkInstance {
        //cleared memory allocation for Vulkans Application Info
        val appInfo = VkApplicationInfo.calloc()
        appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
        appInfo.pApplicationName(memASCII("Hello Triange"))
        appInfo.applicationVersion(VK_MAKE_VERSION(1,0,0))
        appInfo.pEngineName(memASCII("Nami"))
        appInfo.engineVersion(VK_MAKE_VERSION(1,0,0))
        appInfo.apiVersion(VK_API_VERSION_1_0)

        val instanceInfo = VkInstanceCreateInfo.calloc()
        instanceInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
        instanceInfo.pApplicationInfo(appInfo)

        val extensions = getRequiredExtensions()
        instanceInfo.ppEnabledExtensionNames(extensions)

        val handle = MemoryStack.stackPush().use {
            val instancePointer = it.mallocPointer(1)
            if(vkCreateInstance(instanceInfo, null, instancePointer) != VK_SUCCESS) {
                error("Failed to create instance")
            }
            instancePointer[0]
        }
        return VkInstance(handle, instanceInfo)

    }

    private fun getRequiredExtensions(): PointerBuffer {
        val glfwExtensions = glfwGetRequiredInstanceExtensions()!!
        return glfwExtensions
    }

    private fun mainLoop() {
        while(!glfwWindowShouldClose(windowPointer)) {
            glfwPollEvents()
        }


    }

    private fun cleanup() {
        vkDestroyInstance(vkInstance, null)

        glfwDestroyWindow(windowPointer)

        glfwTerminate()
    }
}