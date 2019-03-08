package com.code.gamerg8.nami

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memASCII
import org.lwjgl.vulkan.EXTDebugReport.*
import org.lwjgl.vulkan.VK10.VK_FALSE
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.VK_SUCCESS
import java.nio.LongBuffer

object DebugCallback: VkDebugReportCallbackEXT() {
    private var debugPointer: LongBuffer = LongBuffer.allocate(0)

    override fun invoke(flags: Int, objectType: Int, `object`: Long, location: Long, messageCode: Int, pLayerPrefix: Long, pMessage: Long, pUserData: Long): Int {
        val msg = memASCII(pMessage)
        val layer = memASCII(pLayerPrefix)
        println("Validation layer ($layer): $msg")
        println("\t${VkDebugReportCallbackEXT.getString(pMessage)}")
        return VK_FALSE
    }

    fun createDebugCallback(instance: VkInstance) : Int {
        val createInfo = VkDebugReportCallbackCreateInfoEXT.calloc()
        createInfo.sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT)
        createInfo.flags(VK_DEBUG_REPORT_ERROR_BIT_EXT or VK_DEBUG_REPORT_WARNING_BIT_EXT or VK_DEBUG_REPORT_DEBUG_BIT_EXT)
        createInfo.pfnCallback(DebugCallback)



        debugPointer = MemoryStack.stackMallocLong(1)
        vkCreateDebugReportCallbackEXT(instance, createInfo, null, debugPointer)
        return 0
    }

   fun getDebugPointer() : LongBuffer {
        if(debugPointer.capacity() == 0) {
            error("No pointer set")
        }
        return debugPointer
    }
}