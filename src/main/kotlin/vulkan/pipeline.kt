package com.code.gamerg8.nami.vulkan

import com.code.gamerg8.nami.Util.nullptr
import com.code.gamerg8.nami.Vulkan
import org.lwjgl.BufferUtils
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import java.nio.LongBuffer

class GraphicsPipeline {
    var pipeline: Long = nullptr
    var pipelineLayout: Long = nullptr
    var renderPass: Long = nullptr
    lateinit var imageAvailableSemaphores: Array<Long>
    lateinit var renderFinishedSemaphores: Array<Long>
    var descriptorSetLayout: Long = nullptr

    fun createGraphicsPipeline() {
        val vertShaderCode = javaClass.getResourceAsStream("/shaders/vert.spv").readBytes()
        val fragShaderCode = javaClass.getResourceAsStream("/shaders/frag.spv").readBytes()
        val vertModule = createShaderModule(vertShaderCode)
        val fragModule = createShaderModule(fragShaderCode)

        val vertShaderStageInfo = createShaderStageInfo(VK_SHADER_STAGE_VERTEX_BIT, vertModule, "main")
        val fragShaderStageInfo = createShaderStageInfo(VK_SHADER_STAGE_FRAGMENT_BIT, fragModule, "main")

        val shaderStages = VkPipelineShaderStageCreateInfo.calloc(2).put(vertShaderStageInfo).put(fragShaderStageInfo).flip()

        val vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc()
        vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
        vertexInputInfo.pVertexBindingDescriptions(Vertex.getBindingDescription())
        vertexInputInfo.pVertexAttributeDescriptions(Vertex.getAttributeDescription())

        val inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc()
        inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
        inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
        inputAssembly.primitiveRestartEnable(false)

        val viewport = VkViewport.calloc()
        viewport.x(0f)
        viewport.y(0f)
        viewport.width(Vulkan.device.swapchainExtent.width().toFloat())
        viewport.height(Vulkan.device.swapchainExtent.height().toFloat())
        viewport.minDepth(0f)
        viewport.maxDepth(1f)

        val scissor = VkRect2D.calloc()
        val offset = VkOffset2D.calloc()
        offset.set(0, 0)
        scissor.offset(offset)
        scissor.extent(Vulkan.device.swapchainExtent)

        val viewportState = VkPipelineViewportStateCreateInfo.calloc()
        viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
        viewportState.viewportCount(1)
        viewportState.pViewports(VkViewport.calloc(1).put(viewport).flip())
        viewportState.scissorCount(1)
        viewportState.pScissors(VkRect2D.calloc(1).put(scissor).flip())

        val rasterizer = VkPipelineRasterizationStateCreateInfo.calloc()
        rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
        rasterizer.depthClampEnable(false)
        rasterizer.rasterizerDiscardEnable(false)
        rasterizer.polygonMode(VK_POLYGON_MODE_FILL)
        rasterizer.lineWidth(1f)
        rasterizer.cullMode(VK_CULL_MODE_BACK_BIT)
        rasterizer.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
        rasterizer.depthBiasEnable(false)

        // Multisampling
        val multisampling = VkPipelineMultisampleStateCreateInfo.calloc()
        multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
        multisampling.sampleShadingEnable(false)
        multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
        multisampling.minSampleShading(1f)

        // Color blending
        val colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc()
        colorBlendAttachment.colorWriteMask(VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT)
        colorBlendAttachment.blendEnable(false)

        //Alpha blending:
        colorBlendAttachment.blendEnable(true)
        colorBlendAttachment.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
        colorBlendAttachment.dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
        colorBlendAttachment.colorBlendOp(VK_BLEND_OP_ADD)
        colorBlendAttachment.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
        colorBlendAttachment.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
        colorBlendAttachment.alphaBlendOp(VK_BLEND_OP_ADD)

        val colorBlending = VkPipelineColorBlendStateCreateInfo.calloc()
        colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
        colorBlending.logicOpEnable(false) // TODO: RESEARCH
        colorBlending.logicOp(VK_LOGIC_OP_COPY)
        colorBlending.pAttachments(VkPipelineColorBlendAttachmentState.calloc(1).put(colorBlendAttachment).flip())

        val pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc()
        pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
        val pLayouts = memAllocLong(1).put(descriptorSetLayout).flip() as LongBuffer
        pipelineLayoutInfo.pSetLayouts(pLayouts)

        this.pipelineLayout = MemoryStack.stackPush().use {
            val pLayout = it.mallocLong(1)
            if(vkCreatePipelineLayout(Vulkan.device.logicalDevice, pipelineLayoutInfo, null, pLayout) != VK_SUCCESS)
                error("Failed to create pipeline layout")
            pLayout[0]
        }

        memFree(pLayouts)

        val pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1)
        pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
        pipelineInfo.pStages(shaderStages)
        pipelineInfo.pVertexInputState(vertexInputInfo)
        pipelineInfo.pInputAssemblyState(inputAssembly)
        pipelineInfo.pViewportState(viewportState)
        pipelineInfo.pRasterizationState(rasterizer)
        pipelineInfo.pMultisampleState(multisampling)
        pipelineInfo.pDepthStencilState(null)
        pipelineInfo.pColorBlendState(colorBlending)
        pipelineInfo.pDynamicState(null)

        pipelineInfo.layout(pipelineLayout)
        pipelineInfo.renderPass(renderPass)
        pipelineInfo.subpass(0)

        pipelineInfo.basePipelineHandle(VK_NULL_HANDLE)

        this.pipeline = MemoryStack.stackPush().use {
            val pPipeline = it.mallocLong(1)
            if(vkCreateGraphicsPipelines(Vulkan.device.logicalDevice, VK_NULL_HANDLE, pipelineInfo, null, pPipeline) != VK_SUCCESS)
                error("Failed to create graphics pipeline")
            pPipeline[0]
        }

        vkDestroyShaderModule(Vulkan.device.logicalDevice, vertModule, null)
        vkDestroyShaderModule(Vulkan.device.logicalDevice, fragModule, null)
    }

    fun createRenderPass() {
        val colorAttachment = VkAttachmentDescription.calloc(1)
        colorAttachment.format(Vulkan.device.swapchainImageFormat)
        colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT)

        colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
        colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE)

        colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
        colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)

        colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
        colorAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)

        val colorAttachmentRef = VkAttachmentReference.calloc(1)
        colorAttachmentRef.attachment(0)
        colorAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

        val subpass = VkSubpassDescription.calloc(1)
        subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
        subpass.colorAttachmentCount(1)
        subpass.pColorAttachments(colorAttachmentRef)

        val dependency = VkSubpassDependency.calloc(1)
        dependency.srcSubpass(VK_SUBPASS_EXTERNAL)
        dependency.dstSubpass(0)

        dependency.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
        dependency.srcAccessMask(0)

        dependency.dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
        dependency.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT or VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)

        val renderPassInfo = VkRenderPassCreateInfo.calloc()
        renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
        renderPassInfo.pAttachments(colorAttachment)
        renderPassInfo.pSubpasses(subpass)

        renderPassInfo.pDependencies(dependency)

        this.renderPass = MemoryStack.stackPush().use {
            val pRenderPass = it.mallocLong(1)
            if(vkCreateRenderPass(Vulkan.device.logicalDevice, renderPassInfo, null, pRenderPass) != VK_SUCCESS)
                error("Could not create render pass")
            pRenderPass[0]
        }
    }

    fun createSyncObjects() {
        imageAvailableSemaphores = Array<Long>(Vulkan.MaxFramesInFlight) { nullptr }
        renderFinishedSemaphores = Array<Long>(Vulkan.MaxFramesInFlight) { nullptr }
        Vulkan.inFlightFences = Array<Long>(Vulkan.MaxFramesInFlight) { nullptr }

        val semaphoreInfo = VkSemaphoreCreateInfo.calloc()
        semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)

        val fenceInfo = VkFenceCreateInfo.calloc()
        fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
        fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT)

        MemoryStack.stackPush().use {
            val pAvailable = it.mallocLong(1)
            val pRender = it.mallocLong(1)
            val pFence = it.mallocLong(1)
            for(i in 0 until Vulkan.MaxFramesInFlight) {
                if(vkCreateSemaphore(Vulkan.device.logicalDevice, semaphoreInfo, null, pAvailable) != VK_SUCCESS
                    || vkCreateSemaphore(Vulkan.device.logicalDevice, semaphoreInfo, null, pRender) != VK_SUCCESS
                    || vkCreateFence(Vulkan.device.logicalDevice, fenceInfo, null, pFence) != VK_SUCCESS)
                    error("Failed to create sync objects!")
                imageAvailableSemaphores[i] = pAvailable[0]
                renderFinishedSemaphores[i] = pRender[0]
                Vulkan.inFlightFences[i] = pFence[0]
            }
        }
    }

    fun cleanup(device: Device) {
        vkDestroyDescriptorSetLayout(device.logicalDevice, descriptorSetLayout, null)
        for(i in 0 until Vulkan.MaxFramesInFlight) {
            vkDestroySemaphore(Vulkan.device.logicalDevice, renderFinishedSemaphores[i], null)
            vkDestroySemaphore(Vulkan.device.logicalDevice, imageAvailableSemaphores[i], null)
            vkDestroyFence(Vulkan.device.logicalDevice, Vulkan.inFlightFences[i], null)
        }
        vkDestroyPipeline(device.logicalDevice, pipeline, null)
        vkDestroyPipelineLayout(device.logicalDevice, pipelineLayout, null)
        vkDestroyRenderPass(device.logicalDevice, renderPass, null)
    }

    fun createDescriptorSetLayout() {
        val uboLayoutBinding = VkDescriptorSetLayoutBinding.calloc(1)
        uboLayoutBinding.binding(0)
        uboLayoutBinding.descriptorCount(1)
        uboLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)

        uboLayoutBinding.stageFlags(VK_SHADER_STAGE_VERTEX_BIT)

        val layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc()
        layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
        layoutInfo.pBindings(uboLayoutBinding)

        val pBinding: LongBuffer = memAllocLong(1)
        if(vkCreateDescriptorSetLayout(Vulkan.device.logicalDevice, layoutInfo, null, pBinding) != VK_SUCCESS) {
            error("failed to create descriptor set layout")
        }

        descriptorSetLayout = pBinding[0]
        memFree(pBinding)
    }



    private fun createShaderModule(code: ByteArray): Long {
        val createInfo = VkShaderModuleCreateInfo.calloc()
        createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
        val codeData = BufferUtils.createByteBuffer(code.size)
        codeData.put(code)
        codeData.flip()
        createInfo.pCode(codeData)

        return MemoryStack.stackPush().use {
            val pModule = it.mallocLong(1)
            if(vkCreateShaderModule(Vulkan.device.logicalDevice, createInfo, null, pModule) != VK_SUCCESS)
                error("Failed to create shader module!")
            pModule[0]
        }
    }

    private fun createShaderStageInfo(stage: Int, module: Long, name: String): VkPipelineShaderStageCreateInfo {
        val stageInfo = VkPipelineShaderStageCreateInfo.calloc()
        stageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
        stageInfo.stage(stage)
        stageInfo.module(module)
        stageInfo.pName(memUTF8(name))

        return stageInfo
    }

}