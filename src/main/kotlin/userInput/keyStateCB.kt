package com.code.gamerg8.nami.userInput

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWKeyCallbackI

class keyStateCB(val onKeyDown: Event<Int>, val onKeyRepeat: Event<Int>, val onKeyUp: Event<Int>) : GLFWKeyCallbackI{
    override fun invoke(window: Long, key: Int, scanCode: Int, action: Int, mods: Int){

        when(action) {
            GLFW_PRESS -> onKeyDown(key)
            GLFW_RELEASE -> onKeyUp(key)
            GLFW_REPEAT -> onKeyRepeat(key)
            else -> error("unknown key action")
        }
    }
}

class keyEvent(val key: Int, val scanCode: Int){

}