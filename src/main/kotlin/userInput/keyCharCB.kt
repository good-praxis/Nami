package com.code.gamerg8.nami.userInput

import org.lwjgl.glfw.GLFWCharCallbackI

class keyCharCB(val onKeyChar: Event<Int>) : GLFWCharCallbackI {
    override fun invoke(window: Long, codePoint: Int) {
        onKeyChar(codePoint)
    }
}