package com.code.gamerg8.nami.userInput

import org.lwjgl.glfw.GLFW

class inputManager(window: Long) {
    val onKeyDown = Event<Int>()
    val onKeyRepeat = Event<Int>()
    val onKeyUp = Event<Int>()
    val onKeyChar = Event<Int>()

    init {
        GLFW.glfwSetKeyCallback(window) { window, key, scanCode, action, mods ->
                when(action) {
                    GLFW.GLFW_PRESS -> onKeyDown(key)
                    GLFW.GLFW_RELEASE -> onKeyUp(key)
                    GLFW.GLFW_REPEAT -> onKeyRepeat(key)
                    else -> error("unknown key action")
                }
        }
        GLFW.glfwSetCharCallback(window) { window, codePoint ->
            onKeyChar(codePoint)
        }
        GLFW.glfwSetCursorPosCallback(window) { window, xpos, ypos ->
            println("$xpos $ypos")
        }
        GLFW.glfwSetMouseButtonCallback(window) { window, button, action, mods ->

        }
        GLFW.glfwSetScrollCallback(window) { window, xoffset, yoffset ->

        }
    }
}