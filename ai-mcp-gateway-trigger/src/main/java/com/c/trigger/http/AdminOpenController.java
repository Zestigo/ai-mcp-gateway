package com.c.trigger.http;

import com.c.api.common.Response;
import com.c.api.model.dto.*;
import com.c.api.model.request.GatewayConfigRequestDTO;
import com.c.api.model.response.PageResponse;
import com.c.api.service.AdminOpenService;
import com.c.cases.admin.AdminAuthService;
import com.c.cases.admin.AdminGatewayService;
import com.c.cases.admin.AdminManageService;
import com.c.cases.admin.AdminProtocolService;
import com.c.cases.admin.AdminToolService;
import com.c.domain.admin.model.entity.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员开放接口控制器
 * 提供网关管理、协议管理、工具管理、鉴权管理和网关生命周期等RESTful接口
 *
 * @author cyh
 * @date 2026/03/31
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminOpenController implements AdminOpenService {

    /** 网关管理服务 */
    private final AdminGatewayService adminGatewayService;
    /** 网关鉴权管理服务 */
    private final AdminAuthService adminAuthService;
    /** 协议管理服务 */
    private final AdminProtocolService adminProtocolService;
    /** 工具管理服务 */
    private final AdminToolService adminToolService;
    /** 网关生命周期管理服务 */
    private final AdminManageService adminManageService;

    // ==================== 一、网关管理 ====================

    /**
     * 分页查询网关配置列表
     *
     * @param keyword  搜索关键词
     * @param status   网关状态
     * @param pageNo   页码
     * @param pageSize 每页条数
     * @return 分页网关配置列表
     */
    @Override
    @GetMapping("/gateway/list")
    public Response<PageResponse<GatewayConfigDTO>> queryGatewayList(@RequestParam(required = false) String keyword,
                                                                     @RequestParam(required = false) Integer status,
                                                                     @RequestParam(defaultValue = "1") int pageNo,
                                                                     @RequestParam(defaultValue = "10") int pageSize) {
        log.info("[网关管理] 查询网关列表，keyword={}，status={}，pageNo={}，pageSize={}", keyword, status, pageNo, pageSize);
        try {
            com.c.domain.admin.model.valobj.PageResponse<GatewayConfigEntity> page =
                    adminGatewayService.queryGatewayConfigPage(pageNo, pageSize, keyword, status);
            log.info("[网关管理] 查询网关列表成功，总条数：{}", page.getTotal());
            return Response.success(convertPage(page, this::convertToDTO));
        } catch (Exception e) {
            log.error("[网关管理] 查询网关列表异常", e);
            throw e;
        }
    }

    /**
     * 创建网关配置
     *
     * @param requestDTO 网关创建请求参数
     * @return 创建结果
     */
    @Override
    @PostMapping("/gateway/create")
    public Response<Boolean> createGateway(@RequestBody @Valid GatewayConfigRequestDTO.GatewayConfig requestDTO) {
        log.info("[网关管理] 创建网关，参数：{}", requestDTO);
        try {
            GatewayConfigEntity entity = new GatewayConfigEntity();
            BeanUtils.copyProperties(requestDTO, entity);
            boolean result = adminGatewayService.createGateway(entity);
            log.info("[网关管理] 创建网关完成，结果：{}", result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[网关管理] 创建网关异常，参数：{}", requestDTO, e);
            throw e;
        }
    }

    /**
     * 更新网关配置（带乐观锁）
     *
     * @param requestDTO 网关更新请求参数
     * @param oldVersion 乐观锁版本号
     * @return 更新结果
     */
    @Override
    @PostMapping("/gateway/update")
    public Response<Boolean> updateGateway(@RequestBody @Valid GatewayConfigRequestDTO.GatewayConfig requestDTO,
                                           @RequestParam Long oldVersion) {
        log.info("[网关管理] 更新网关，参数：{}，乐观锁版本：{}", requestDTO, oldVersion);
        try {
            GatewayConfigEntity entity = new GatewayConfigEntity();
            BeanUtils.copyProperties(requestDTO, entity);
            boolean result = adminGatewayService.updateGateway(entity, oldVersion);
            log.info("[网关管理] 更新网关完成，结果：{}", result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[网关管理] 更新网关异常，参数：{}", requestDTO, e);
            throw e;
        }
    }

    /**
     * 删除网关配置
     *
     * @param gatewayId 网关ID
     * @return 删除结果
     */
    @Override
    @DeleteMapping("/gateway/delete/{gatewayId}")
    public Response<Boolean> deleteGateway(@PathVariable String gatewayId) {
        log.info("[网关管理] 删除网关，gatewayId：{}", gatewayId);
        try {
            boolean result = adminGatewayService.deleteGateway(gatewayId);
            log.info("[网关管理] 删除网关完成，gatewayId：{}，结果：{}", gatewayId, result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[网关管理] 删除网关异常，gatewayId：{}", gatewayId, e);
            throw e;
        }
    }

    /**
     * 查询单个网关详情
     *
     * @param gatewayId 网关ID
     * @return 网关详情
     */
    @Override
    @GetMapping("/gateway/get/{gatewayId}")
    public Response<GatewayConfigDTO> getGateway(@PathVariable String gatewayId) {
        log.info("[网关管理] 查询单个网关详情，gatewayId：{}", gatewayId);
        try {
            GatewayConfigDTO dto = convertToDTO(adminGatewayService.findGatewayById(gatewayId));
            log.info("[网关管理] 查询网关详情成功，gatewayId：{}", gatewayId);
            return Response.success(dto);
        } catch (Exception e) {
            log.error("[网关管理] 查询网关详情异常，gatewayId：{}", gatewayId, e);
            throw e;
        }
    }

    /**
     * 启用网关（带乐观锁）
     *
     * @param gatewayId  网关ID
     * @param oldVersion 乐观锁版本号
     * @return 启用结果
     */
    @Override
    @PostMapping("/gateway/enable/{gatewayId}")
    public Response<Boolean> enableGateway(@PathVariable String gatewayId, @RequestParam Long oldVersion) {
        log.info("[网关管理] 启用网关，gatewayId：{}，oldVersion：{}", gatewayId, oldVersion);
        try {
            boolean result = adminGatewayService.enableGateway(gatewayId, oldVersion);
            log.info("[网关管理] 启用网关完成，gatewayId：{}，结果：{}", gatewayId, result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[网关管理] 启用网关异常，gatewayId：{}", gatewayId, e);
            throw e;
        }
    }

    /**
     * 禁用网关（带乐观锁）
     *
     * @param gatewayId  网关ID
     * @param oldVersion 乐观锁版本号
     * @return 禁用结果
     */
    @Override
    @PostMapping("/gateway/disable/{gatewayId}")
    public Response<Boolean> disableGateway(@PathVariable String gatewayId, @RequestParam Long oldVersion) {
        log.info("[网关管理] 禁用网关，gatewayId：{}，oldVersion：{}", gatewayId, oldVersion);
        try {
            boolean result = adminGatewayService.disableGateway(gatewayId, oldVersion);
            log.info("[网关管理] 禁用网关完成，gatewayId：{}，结果：{}", gatewayId, result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[网关管理] 禁用网关异常，gatewayId：{}", gatewayId, e);
            throw e;
        }
    }

    // ==================== 二、协议管理 ====================

    /**
     * 分页查询协议配置列表
     *
     * @param protocolId 协议ID
     * @param status     协议状态
     * @param pageNo     页码
     * @param pageSize   每页条数
     * @return 分页协议配置列表
     */
    @Override
    @GetMapping("/protocol/list")
    public Response<PageResponse<GatewayProtocolDTO>> queryProtocolList(@RequestParam(required = false) Long protocolId, @RequestParam(required = false) Integer status, @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "10") int pageSize) {
        log.info("[协议管理] 查询协议列表，protocolId={}，status={}，pageNo={}，pageSize={}", protocolId, status, pageNo, pageSize);
        try {
            com.c.domain.admin.model.valobj.PageResponse<GatewayProtocolEntity> page =
                    adminProtocolService.queryProtocolConfigPage(pageNo, pageSize, protocolId, status);
            log.info("[协议管理] 查询协议列表成功，总条数：{}", page.getTotal());
            return Response.success(convertPage(page, this::convertToDTO));
        } catch (Exception e) {
            log.error("[协议管理] 查询协议列表异常", e);
            throw e;
        }
    }

    /**
     * 查询全部协议配置
     *
     * @param protocolId 协议ID
     * @param status     协议状态
     * @return 协议配置列表
     */
    @Override
    @GetMapping("/protocol/listAll")
    public Response<List<GatewayProtocolDTO>> listAllProtocols(@RequestParam(required = false) Long protocolId,
                                                               @RequestParam(required = false) Integer status) {
        log.info("[协议管理] 查询全部协议，protocolId={}，status={}", protocolId, status);
        try {
            List<GatewayProtocolEntity> list = adminProtocolService.getProtocolConfigList(protocolId, status);
            log.info("[协议管理] 查询全部协议成功，条数：{}", list.size());
            return Response.success(list
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("[协议管理] 查询全部协议异常", e);
            throw e;
        }
    }

    /**
     * 创建HTTP协议配置
     *
     * @param requestDTO 协议创建请求参数
     * @return 创建结果
     */
    @Override
    @PostMapping("/protocol/createHttp")
    public Response<Boolean> createProtocolHttp(@RequestBody @Valid GatewayConfigRequestDTO requestDTO) {
        log.info("[协议管理] 创建HTTP协议，参数：{}", requestDTO);
        try {
            GatewayProtocolEntity entity = new GatewayProtocolEntity();
            BeanUtils.copyProperties(requestDTO, entity);
            boolean result = adminProtocolService.createProtocol(entity);
            log.info("[协议管理] 创建HTTP协议完成，结果：{}", result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[协议管理] 创建HTTP协议异常，参数：{}", requestDTO, e);
            throw e;
        }
    }

    /**
     * 更新HTTP协议配置（带乐观锁）
     *
     * @param requestDTO 协议更新请求参数
     * @param oldVersion 乐观锁版本号
     * @return 更新结果
     */
    @Override
    @PostMapping("/protocol/updateHttp")
    public Response<Boolean> updateProtocolHttp(@RequestBody @Valid GatewayConfigRequestDTO requestDTO,
                                                @RequestParam Long oldVersion) {
        log.info("[协议管理] 更新HTTP协议，参数：{}，乐观锁版本：{}", requestDTO, oldVersion);
        try {
            GatewayProtocolEntity entity = new GatewayProtocolEntity();
            BeanUtils.copyProperties(requestDTO, entity);
            boolean result = adminProtocolService.updateProtocolConfig(entity, oldVersion);
            log.info("[协议管理] 更新HTTP协议完成，结果：{}", result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[协议管理] 更新HTTP协议异常，参数：{}", requestDTO, e);
            throw e;
        }
    }

    /**
     * 根据协议ID查询协议详情
     *
     * @param protocolId 协议ID
     * @return 协议详情
     */
    @Override
    @GetMapping("/protocol/get/{protocolId}")
    public Response<GatewayProtocolDTO> getProtocolById(@PathVariable Long protocolId) {
        log.info("[协议管理] 查询协议详情，protocolId：{}", protocolId);
        try {
            GatewayProtocolDTO dto = convertToDTO(adminProtocolService.findProtocolById(protocolId));
            log.info("[协议管理] 查询协议详情成功，protocolId：{}", protocolId);
            return Response.success(dto);
        } catch (Exception e) {
            log.error("[协议管理] 查询协议详情异常，protocolId：{}", protocolId, e);
            throw e;
        }
    }

    /**
     * 删除协议配置
     *
     * @param protocolId 协议ID
     * @return 删除结果
     */
    @Override
    @DeleteMapping("/protocol/delete/{protocolId}")
    public Response<Boolean> deleteProtocol(@PathVariable Long protocolId) {
        log.info("[协议管理] 删除协议，protocolId：{}", protocolId);
        try {
            boolean result = adminProtocolService.deleteProtocolById(protocolId);
            log.info("[协议管理] 删除协议完成，protocolId：{}，结果：{}", protocolId, result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[协议管理] 删除协议异常，protocolId：{}", protocolId, e);
            throw e;
        }
    }

    /**
     * 修改协议状态（带乐观锁）
     *
     * @param protocolId 协议ID
     * @param status     目标状态
     * @param oldVersion 乐观锁版本号
     * @return 修改结果
     */
    @Override
    @PostMapping("/protocol/updateStatus")
    public Response<Boolean> updateProtocolStatus(@RequestParam Long protocolId, @RequestParam Integer status,
                                                  @RequestParam Long oldVersion) {
        log.info("[协议管理] 修改协议状态，protocolId={}，status={}，oldVersion={}", protocolId, status, oldVersion);
        try {
            boolean result = adminProtocolService.updateProtocolStatus(protocolId, status, oldVersion);
            log.info("[协议管理] 修改协议状态完成，结果：{}", result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[协议管理] 修改协议状态异常", e);
            throw e;
        }
    }

    // ==================== 三、工具管理 ====================

    /**
     * 分页查询网关工具配置列表
     *
     * @param gatewayId  网关ID
     * @param protocolId 协议ID
     * @param toolStatus 工具状态
     * @param pageNo     页码
     * @param pageSize   每页条数
     * @return 分页工具配置列表
     */
    @Override
    @GetMapping("/tool/list")
    public Response<PageResponse<GatewayToolConfigDTO>> queryToolList(@RequestParam(required = false) String gatewayId, @RequestParam(required = false) Long protocolId, @RequestParam(required = false) Integer toolStatus, @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "10") int pageSize) {
        log.info("[工具管理] 查询工具列表，gatewayId={}，protocolId={}，toolStatus={}，pageNo={}，pageSize={}", gatewayId,
                protocolId, toolStatus, pageNo, pageSize);
        try {
            com.c.domain.admin.model.valobj.PageResponse<GatewayToolConfigEntity> page =
                    adminToolService.queryToolPage(pageNo, pageSize, gatewayId, protocolId, toolStatus);
            log.info("[工具管理] 查询工具列表成功，总条数：{}", page.getTotal());
            return Response.success(convertPage(page, this::convertToDTO));
        } catch (Exception e) {
            log.error("[工具管理] 查询工具列表异常", e);
            throw e;
        }
    }

    /**
     * 根据网关ID查询绑定的工具列表
     *
     * @param gatewayId 网关ID
     * @return 工具配置列表
     */
    @Override
    @GetMapping("/tool/listByGateway")
    public Response<List<GatewayToolConfigDTO>> listToolsByGateway(@RequestParam String gatewayId) {
        log.info("[工具管理] 根据网关查询工具，gatewayId：{}", gatewayId);
        try {
            List<GatewayToolConfigEntity> list = adminToolService.listToolsByGateway(gatewayId);
            log.info("[工具管理] 根据网关查询工具成功，条数：{}", list.size());
            return Response.success(list
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("[工具管理] 根据网关查询工具异常，gatewayId：{}", gatewayId, e);
            throw e;
        }
    }

    /**
     * 绑定网关工具
     *
     * @param requestDTO 工具绑定请求参数
     * @return 绑定结果
     */
    @Override
    @PostMapping("/tool/bind")
    public Response<Boolean> bindTool(@RequestBody @Valid GatewayConfigRequestDTO.GatewayToolConfig requestDTO) {
        log.info("[工具管理] 绑定工具，参数：{}", requestDTO);
        try {
            GatewayToolConfigEntity entity = new GatewayToolConfigEntity();
            BeanUtils.copyProperties(requestDTO, entity);
            boolean result = adminToolService.bindTool(entity);
            log.info("[工具管理] 绑定工具完成，结果：{}", result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[工具管理] 绑定工具异常，参数：{}", requestDTO, e);
            throw e;
        }
    }

    /**
     * 更新网关工具配置（带乐观锁）
     *
     * @param requestDTO 工具更新请求参数
     * @param oldVersion 乐观锁版本号
     * @return 更新结果
     */
    @Override
    @PostMapping("/tool/updateTool")
    public Response<Boolean> updateToolConfig(@RequestBody @Valid GatewayConfigRequestDTO.GatewayToolConfig requestDTO, @RequestParam Long oldVersion) {
        log.info("[工具管理] 更新工具配置，参数：{}，乐观锁版本：{}", requestDTO, oldVersion);
        try {
            // 对象转换
            GatewayToolConfigEntity entity = new GatewayToolConfigEntity();
            BeanUtils.copyProperties(requestDTO, entity);
            // 调用领域服务层（带乐观锁）
            boolean result = adminToolService.updateToolConfig(entity, oldVersion);
            log.info("[工具管理] 更新工具配置完成，结果：{}", result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[工具管理] 更新工具配置异常，参数：{}", requestDTO, e);
            throw e;
        }
    }

    /**
     * 解绑网关工具
     *
     * @param gatewayId  网关ID
     * @param toolId     工具ID
     * @param protocolId 协议ID
     * @return 解绑结果
     */
    @Override
    @PostMapping("/tool/unbind")
    public Response<Boolean> unbindTool(@RequestParam String gatewayId, @RequestParam Integer toolId,
                                        @RequestParam Long protocolId) {
        log.info("[工具管理] 解绑工具，gatewayId={}，toolId={}，protocolId={}", gatewayId, toolId, protocolId);
        try {
            boolean result = adminToolService.unbindTool(gatewayId, toolId, protocolId);
            log.info("[工具管理] 解绑工具完成，结果：{}", result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[工具管理] 解绑工具异常", e);
            throw e;
        }
    }

    /**
     * 检查工具是否已绑定到网关
     *
     * @param gatewayId  网关ID
     * @param toolId     工具ID
     * @param protocolId 协议ID
     * @return 存在结果
     */
    @Override
    @GetMapping("/tool/checkExist")
    public Response<Boolean> checkToolExist(@RequestParam String gatewayId, @RequestParam Integer toolId,
                                            @RequestParam Long protocolId) {
        log.info("[工具管理] 检查工具是否存在，gatewayId={}，toolId={}，protocolId={}", gatewayId, toolId, protocolId);
        try {
            boolean result = adminToolService.isToolExist(gatewayId, toolId, protocolId);
            log.info("[工具管理] 检查工具是否存在完成，结果：{}", result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[工具管理] 检查工具是否存在异常", e);
            throw e;
        }
    }

    /**
     * 启用网关工具（带乐观锁）
     *
     * @param gatewayId  网关ID
     * @param toolId     工具ID
     * @param protocolId 协议ID
     * @param oldVersion 乐观锁版本号
     * @return 启用结果
     */
    @Override
    @PostMapping("/tool/enable")
    public Response<Boolean> enableTool(@RequestParam String gatewayId, @RequestParam Integer toolId,
                                        @RequestParam Long protocolId, @RequestParam Long oldVersion) {
        log.info("[工具管理] 启用工具，gatewayId={}，toolId={}，protocolId={}，oldVersion={}", gatewayId, toolId, protocolId,
                oldVersion);
        try {
            boolean result = adminToolService.enableTool(gatewayId, toolId, protocolId, oldVersion);
            log.info("[工具管理] 启用工具完成，结果：{}", result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[工具管理] 启用工具异常", e);
            throw e;
        }
    }

    /**
     * 禁用网关工具（带乐观锁）
     *
     * @param gatewayId  网关ID
     * @param toolId     工具ID
     * @param protocolId 协议ID
     * @param oldVersion 乐观锁版本号
     * @return 禁用结果
     */
    @Override
    @PostMapping("/tool/disable")
    public Response<Boolean> disableTool(@RequestParam String gatewayId, @RequestParam Integer toolId,
                                         @RequestParam Long protocolId, @RequestParam Long oldVersion) {
        log.info("[工具管理] 禁用工具，gatewayId={}，toolId={}，protocolId={}，oldVersion={}", gatewayId, toolId, protocolId,
                oldVersion);
        try {
            boolean result = adminToolService.disableTool(gatewayId, toolId, protocolId, oldVersion);
            log.info("[工具管理] 禁用工具完成，结果：{}", result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[工具管理] 禁用工具异常", e);
            throw e;
        }
    }

    // ==================== 四、网关鉴权 API Key ====================

    /**
     * 分页查询网关API密钥列表
     *
     * @param gatewayId 网关ID
     * @param pageNo    页码
     * @param pageSize  每页条数
     * @return 分页API密钥列表
     */
    @Override
    @GetMapping("/auth/apiKeyList")
    public Response<PageResponse<GatewayAuthDTO>> queryGatewayAuthPage(@RequestParam(required = false) String gatewayId, @RequestParam(defaultValue = "1") int pageNo, @RequestParam(defaultValue = "10") int pageSize) {
        log.info("[鉴权管理] 查询ApiKey列表，gatewayId={}，pageNo={}，pageSize={}", gatewayId, pageNo, pageSize);
        try {
            com.c.domain.admin.model.valobj.PageResponse<GatewayAuthEntity> page =
                    adminAuthService.queryGatewayAuthPage(pageNo, pageSize, gatewayId);
            log.info("[鉴权管理] 查询ApiKey列表成功，总条数：{}", page.getTotal());
            return Response.success(convertPage(page, this::convertToDTO));
        } catch (Exception e) {
            log.error("[鉴权管理] 查询ApiKey列表异常", e);
            throw e;
        }
    }

    /**
     * 创建网关API密钥
     *
     * @param requestDTO API密钥创建请求参数
     * @return 创建结果
     */
    @Override
    @PostMapping("/auth/createApiKey")
    public Response<Boolean> createApiKey(@RequestBody @Valid GatewayConfigRequestDTO.GatewayAuth requestDTO) {
        log.info("[鉴权管理] 创建ApiKey，参数：{}", requestDTO);
        try {
            GatewayAuthEntity entity = new GatewayAuthEntity();
            BeanUtils.copyProperties(requestDTO, entity);
            boolean result = adminAuthService.saveAuth(entity);
            log.info("[鉴权管理] 创建ApiKey完成，结果：{}", result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[鉴权管理] 创建ApiKey异常，参数：{}", requestDTO, e);
            throw e;
        }
    }

    /**
     * 吊销网关API密钥
     *
     * @param gatewayId 网关ID
     * @param apiKey    API密钥
     * @return 吊销结果
     */
    @Override
    @PostMapping("/auth/revokeApiKey")
    public Response<Boolean> revokeApiKey(@RequestParam String gatewayId, @RequestParam String apiKey) {
        log.info("[鉴权管理] 吊销ApiKey，gatewayId={}，apiKey={}", gatewayId, apiKey);
        try {
            boolean result = adminAuthService.revokeApiKey(gatewayId, apiKey);
            log.info("[鉴权管理] 吊销ApiKey完成，结果：{}", result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[鉴权管理] 吊销ApiKey异常", e);
            throw e;
        }
    }

    /**
     * 启用API密钥（带乐观锁）
     *
     * @param gatewayId  网关ID
     * @param apiKey     API密钥
     * @param oldVersion 乐观锁版本号
     * @return 启用结果
     */
    @Override
    @PostMapping("/auth/enableApiKey")
    public Response<Boolean> enableApiKey(@RequestParam String gatewayId, @RequestParam String apiKey,
                                          @RequestParam Long oldVersion) {
        log.info("[鉴权管理] 启用ApiKey，gatewayId={}，apiKey={}，oldVersion={}", gatewayId, apiKey, oldVersion);
        try {
            boolean result = adminAuthService.enableApiKey(gatewayId, apiKey, oldVersion);
            log.info("[鉴权管理] 启用ApiKey完成，结果：{}", result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[鉴权管理] 启用ApiKey异常", e);
            throw e;
        }
    }

    /**
     * 禁用API密钥（带乐观锁）
     *
     * @param gatewayId  网关ID
     * @param apiKey     API密钥
     * @param oldVersion 乐观锁版本号
     * @return 禁用结果
     */
    @Override
    @PostMapping("/auth/disableApiKey")
    public Response<Boolean> disableApiKey(@RequestParam String gatewayId, @RequestParam String apiKey,
                                           @RequestParam Long oldVersion) {
        log.info("[鉴权管理] 禁用ApiKey，gatewayId={}，apiKey={}，oldVersion={}", gatewayId, apiKey, oldVersion);
        try {
            boolean result = adminAuthService.disableApiKey(gatewayId, apiKey, oldVersion);
            log.info("[鉴权管理] 禁用ApiKey完成，结果：{}", result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[鉴权管理] 禁用ApiKey异常", e);
            throw e;
        }
    }

    /**
     * 查询API密钥详情
     *
     * @param apiKey API密钥
     * @return 密钥详情
     */
    @Override
    @GetMapping("/auth/apiKeyDetail")
    public Response<GatewayAuthDTO> getApiKeyDetail(@RequestParam String apiKey) {
        log.info("[鉴权管理] 查询ApiKey详情，apiKey：{}", apiKey);
        try {
            GatewayAuthDTO dto = convertToDTO(adminAuthService.findAuthByApiKey(apiKey));
            log.info("[鉴权管理] 查询ApiKey详情成功");
            return Response.success(dto);
        } catch (Exception e) {
            log.error("[鉴权管理] 查询ApiKey详情异常", e);
            throw e;
        }
    }

    /**
     * 检查API密钥是否存在
     *
     * @param apiKey API密钥
     * @return 存在结果
     */
    @Override
    @GetMapping("/auth/checkApiKeyExist")
    public Response<Boolean> checkApiKeyExist(@RequestParam String apiKey) {
        log.info("[鉴权管理] 检查ApiKey是否存在，apiKey：{}", apiKey);
        try {
            boolean result = adminAuthService.isApiKeyExists(apiKey);
            log.info("[鉴权管理] 检查ApiKey是否存在完成，结果：{}", result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[鉴权管理] 检查ApiKey是否存在异常", e);
            throw e;
        }
    }

    // ==================== 五、网关生命周期 ====================

    /**
     * 发布网关
     *
     * @param gatewayId 网关ID
     * @return 发布结果
     */
    @Override
    @PostMapping("/gateway/publish/{gatewayId}")
    public Response<Boolean> publishGateway(@PathVariable String gatewayId) {
        log.info("[网关生命周期] 发布网关，gatewayId：{}", gatewayId);
        try {
            boolean result = adminManageService.publishGateway(gatewayId);
            log.info("[网关生命周期] 发布网关完成，gatewayId：{}，结果：{}", gatewayId, result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[网关生命周期] 发布网关异常，gatewayId：{}", gatewayId, e);
            throw e;
        }
    }

    /**
     * 下线网关
     *
     * @param gatewayId 网关ID
     * @return 下线结果
     */
    @Override
    @PostMapping("/gateway/offline/{gatewayId}")
    public Response<Boolean> offlineGateway(@PathVariable String gatewayId) {
        log.info("[网关生命周期] 下线网关，gatewayId：{}", gatewayId);
        try {
            boolean result = adminManageService.offlineGateway(gatewayId);
            log.info("[网关生命周期] 下线网关完成，gatewayId：{}，结果：{}", gatewayId, result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[网关生命周期] 下线网关异常，gatewayId：{}", gatewayId, e);
            throw e;
        }
    }

    /**
     * 刷新/同步网关配置
     *
     * @param gatewayId 网关ID
     * @return 刷新结果
     */
    @Override
    @PostMapping("/gateway/refresh/{gatewayId}")
    public Response<Boolean> refreshGateway(@PathVariable String gatewayId) {
        log.info("[网关生命周期] 刷新网关配置，gatewayId：{}", gatewayId);
        try {
            boolean result = adminManageService.syncGatewayConfig(gatewayId);
            log.info("[网关生命周期] 刷新网关配置完成，gatewayId：{}，结果：{}", gatewayId, result);
            return Response.success(result);
        } catch (Exception e) {
            log.error("[网关生命周期] 刷新网关配置异常，gatewayId：{}", gatewayId, e);
            throw e;
        }
    }

    // ==================== 工具转换方法 ====================

    /**
     * 网关配置实体转DTO
     */
    private GatewayConfigDTO convertToDTO(GatewayConfigEntity e) {
        if (e == null) return null;
        GatewayConfigDTO dto = new GatewayConfigDTO();
        BeanUtils.copyProperties(e, dto);
        return dto;
    }

    /**
     * 网关工具配置实体转DTO
     */
    private GatewayToolConfigDTO convertToDTO(GatewayToolConfigEntity e) {
        if (e == null) return null;
        GatewayToolConfigDTO dto = new GatewayToolConfigDTO();
        BeanUtils.copyProperties(e, dto);
        dto.setStatus(e.getToolStatus());
        return dto;
    }

    /**
     * 协议配置实体转DTO
     */
    private GatewayProtocolDTO convertToDTO(GatewayProtocolEntity e) {
        if (e == null) return null;
        GatewayProtocolDTO dto = new GatewayProtocolDTO();
        BeanUtils.copyProperties(e, dto);
        return dto;
    }

    /**
     * 网关鉴权实体转DTO
     */
    private GatewayAuthDTO convertToDTO(GatewayAuthEntity e) {
        if (e == null) return null;
        GatewayAuthDTO dto = new GatewayAuthDTO();
        BeanUtils.copyProperties(e, dto);
        return dto;
    }

    /**
     * 分页对象通用转换
     */
    private <E, D> PageResponse<D> convertPage(com.c.domain.admin.model.valobj.PageResponse<E> sourcePage,
                                               java.util.function.Function<E, D> converter) {
        PageResponse<D> target = new PageResponse<>();
        if (sourcePage == null || sourcePage.getList() == null || sourcePage
                .getList()
                .isEmpty()) {
            target.setList(Collections.emptyList());
            target.setTotal(0L);
            target.setPageNo(1);
            target.setPageSize(10);
            return target;
        }
        target.setList(sourcePage
                .getList()
                .stream()
                .map(converter)
                .collect(Collectors.toList()));
        target.setTotal(sourcePage.getTotal());
        target.setPageNo(sourcePage.getPageNo());
        target.setPageSize(sourcePage.getPageSize());
        return target;
    }
}