package com.code.gamerg8.nami

import com.code.gamerg8.nami.timeKeeper.TimeKeeper
import com.code.gamerg8.nami.vulkan.Vulkan
import java.util.*

fun main(args: Array<String>) {
    // 3 threads: draw thread, gamelogic thread, input thread
    Vulkan.run()
    Vulkan.window.inputManager.onKeyDown += ::OnKeyDown
}

fun init() {

}

fun OnKeyDown(x: Int){
    println(x)
}