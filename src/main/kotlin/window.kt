package com.code.gamerg8.nami

import org.lwjgl.PointerBuffer
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWVulkan


class Window(private val width: Int, private val height: Int) {
    private var windowPointer: Long = -1

    fun getVulkanWindow(): Long {
        initWindow()
        createWindow()
        return this.windowPointer
    }

    fun destroy() {
        glfwDestroyWindow(this.windowPointer)
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