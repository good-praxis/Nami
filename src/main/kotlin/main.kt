package com.code.gamerg8.nami

import com.code.gamerg8.nami.timeKeeper.TimeKeeper
import com.code.gamerg8.nami.vulkan.Vulkan
import java.util.*

var count = 0
var currentDir = 0


fun main(args: Array<String>) {
    // 3 threads: draw thread, gamelogic thread, input thread
    Vulkan.run(gameLoop())
}

fun gameLoop() {
    Vulkan.tempRotation = currentDir
}

fun onKeyDown(x: Int){
    if(x == 65) { //w
        currentDir = -1
    } else if(x == 68) { //d
        currentDir = 1
    }
}

fun onKeyUp(x: Int) {
    if(x == 65 || x == 68) {
        currentDir = 0
    }
}
