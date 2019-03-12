package com.code.gamerg8.nami.userInput

import org.lwjgl.glfw.GLFW

class inputManager(window: Long) {
    val onKeyDown = Event<Int>()
    val onKeyRepeat = Event<Int>()
    val onKeyUp = Event<Int>()
    val onKeyChar = Event<Int>()

    private val keyStateCB: keyStateCB
    private val keyCharCB: keyCharCB

    init {
        keyStateCB = keyStateCB(onKeyDown, onKeyRepeat, onKeyUp)
        GLFW.glfwSetKeyCallback(window, keyStateCB)
        keyCharCB = keyCharCB(onKeyChar)
        GLFW.glfwSetCharCallback(window, keyCharCB)
    }
}