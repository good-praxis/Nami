package com.code.gamerg8.nami

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryUtil.*
import java.lang.Exception

class HelloTriangle {
    val nullptr = NULL
    private val WIDTH = 800
    private val HEIGHT = 600
    private var windowPointer: Long = -1


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

    }

    private fun mainLoop() {
        while(!glfwWindowShouldClose(windowPointer)) {
            glfwPollEvents()
        }

    }

    private fun cleanup() {
        glfwDestroyWindow(windowPointer)

        glfwTerminate()
    }
}

fun main(args: Array<String>) {
    val app = HelloTriangle()

    app.run()

}