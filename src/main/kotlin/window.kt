package com.code.gamerg8.nami

import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.KHRSurface
import org.lwjgl.vulkan.VK10.VK_SUCCESS

class Window(private val width: Int, private val height: Int) {
    var surface: Long = Util.nullptr
    private var windowPointer: Long = -1


    fun getVulkanWindow(): Long {
        initWindow()
        createWindow()
        return getWindowPointer()
    }

    fun getWindowPointer(): Long {
        return windowPointer
    }

    fun getVulkanSurface(): Long {
        createSurface()
        return this.surface
    }

    fun destroy() {
        glfwDestroyWindow(this.windowPointer)
    }

    fun destroySurface() {
        KHRSurface.vkDestroySurfaceKHR(Vulkan.getVkInstance(), this.surface, null)
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
        hintApi(GLFW_RESIZABLE, GLFW_FALSE)
    }

    private fun createWindow() {
        this.windowPointer = glfwCreateWindow(this.width, this.height, "Vulkan", Util.nullptr, Util.nullptr)
    }

    private fun createSurface() {
        MemoryStack.stackPush().use {
            val pSurface = it.mallocLong(1)
            glfwCreateWindowSurface(Vulkan.getVkInstance(), windowPointer, null, pSurface)
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