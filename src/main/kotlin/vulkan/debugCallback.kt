package com.code.gamerg8.nami.vulkan

import com.code.gamerg8.nami.Util
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memASCII
import org.lwjgl.vulkan.EXTDebugReport.*
import org.lwjgl.vulkan.VK10.VK_FALSE
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.VK_SUCCESS

object DebugCallback: VkDebugReportCallbackEXT() {
    private var debugMessenger: Long = Util.nullptr

    override fun invoke(flags: Int, objectType: Int, `object`: Long, location: Long, messageCode: Int, pLayerPrefix: Long, pMessage: Long, pUserData: Long): Int {
        val msg = memASCII(pMessage)
        val layer = memASCII(pLayerPrefix)
        println("Validation layer ($layer): $msg")
        println("\t${VkDebugReportCallbackEXT.getString(pMessage)}")
        return VK_FALSE
    }

    fun createDebugCallback() {
        val createInfo = VkDebugReportCallbackCreateInfoEXT.calloc()
        createInfo.sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT)
        createInfo.flags(VK_DEBUG_REPORT_ERROR_BIT_EXT or VK_DEBUG_REPORT_WARNING_BIT_EXT or VK_DEBUG_REPORT_DEBUG_BIT_EXT)
        createInfo.pfnCallback(DebugCallback)



        val debugPointer = MemoryStack.stackMallocLong(1)
        if (vkCreateDebugReportCallbackEXT(Vulkan.vkInstance, createInfo, null, debugPointer) != VK_SUCCESS){
            error("Couldn't initalized Debug Messenger")
        }
        debugMessenger = debugPointer[0]
    }

    fun cleanup() {
        vkDestroyDebugReportCallbackEXT(Vulkan.vkInstance, debugMessenger, null)
    }

   fun getDebugPointer() : Long {
        if(debugMessenger == Util.nullptr) {
            error("No pointer set")
        }
        return debugMessenger
    }
}