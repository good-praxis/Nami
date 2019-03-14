package com.code.gamerg8.nami

import com.code.gamerg8.nami.userInput.inputManager
import com.code.gamerg8.nami.vulkan.Vulkan
import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface
import org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface

class Window(private val width: Int, private val height: Int) {
    var surface: Long = Util.nullptr
    private var windowPointer: Long = -1
    val inputManager: inputManager

    init {
        initWindow()
        createWindow()
        inputManager = inputManager(windowPointer)
    }

    fun getWindowPointer(): Long {
        return windowPointer
    }

    fun getVulkanSurface(): Long {
        checkCompatibility()
        createSurface()
        return this.surface
    }

    fun destroy() {
        glfwDestroyWindow(this.windowPointer)
    }

    fun destroySurface() {
        KHRSurface.vkDestroySurfaceKHR(Vulkan.vkInstance, this.surface, null)
    }

    fun destroyAndTerminate() {
        destroy()
        terminateApi()
    }

    fun shouldWindowClose(): Boolean {
        return glfwWindowShouldClose(this.windowPointer)
    }

    fun pollEvents() {
        glfwPollEvents()
    }

    private fun initWindow() {
        initApi()

        hintApi(GLFW_CLIENT_API, GLFW_NO_API)
        hintApi(GLFW_RESIZABLE, GLFW_TRUE)
    }

    private fun createWindow() {
        this.windowPointer = glfwCreateWindow(this.width, this.height, "Vulkan", Util.nullptr, Util.nullptr)
    }

    private fun checkCompatibility() {
        if(!glfwVulkanSupported()) {
            error("Vulkan isn't supported on your device.")
        }
    }

    private fun createSurface() {
        MemoryStack.stackPush().use {
            val pSurface = it.mallocLong(1)
            glfwCreateWindowSurface(Vulkan.vkInstance, windowPointer, null, pSurface)
            this.surface = pSurface[0]
        }
    }

    private fun initApi() {
        glfwInit()
    }

    private fun terminateApi() {
        glfwTerminate()
    }

    private fun hintApi(key: Int, value: Int) {
        glfwWindowHint(key, value)
    }

    companion object {
        fun getRequiredExtensions(): PointerBuffer {
            return GLFWVulkan.glfwGetRequiredInstanceExtensions()!!
        }
    }
}