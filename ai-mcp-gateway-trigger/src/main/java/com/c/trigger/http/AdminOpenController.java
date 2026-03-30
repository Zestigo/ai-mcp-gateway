package com.c.trigger.http;

import com.c.api.common.Response;
import com.c.api.model.dto.GatewayConfigDTO;
import com.c.api.model.request.GatewayConfigRequestDTO;
import com.c.api.model.response.GatewayConfigResponseDTO;
import com.c.api.service.AdminOpenService;
import com.c.cases.admin.AdminAuthService;
import com.c.cases.admin.AdminGatewayService;
import com.c.cases.admin.AdminManageService;
import com.c.cases.admin.AdminProtocolService;
import com.c.domain.admin.model.entity.GatewayConfigEntity;
import com.c.domain.auth.model.entity.RegisterCommandEntity;
import com.c.domain.gateway.model.entity.GatewayConfigCommandEntity;
import com.c.domain.gateway.model.entity.GatewayToolConfigCommandEntity;
import com.c.domain.gateway.model.valobj.GatewayConfigVO;
import com.c.domain.gateway.model.valobj.GatewayToolConfigVO;
import com.c.domain.protocol.model.entity.StorageCommandEntity;
import com.c.domain.protocol.model.valobj.http.HTTPProtocolVO;
import com.c.types.enums.GatewayEnum;
import com.c.types.enums.ResponseCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 管理后台开放API控制器
 * 系统接入层（Trigger），统一接收管理端HTTP请求
 * 职责：参数接收 → DTO→Command/VO转换 → 调用应用服务 → 统一响应返回
 * 全链路同步阻塞模型，适配标准Spring MVC环境，避免响应式不执行问题
 *
 * @author cyh
 * @date 2026/03/30
 */
@Slf4j
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
@RequestMapping("/admin/")
public class AdminOpenController implements AdminOpenService {

    /** 网关基础配置应用服务 */
    @Resource
    private AdminGatewayService adminGatewayService;

    /** 网关认证配置应用服务 */
    @Resource
    private AdminAuthService adminAuthService;

    /** 网关协议配置应用服务 */
    @Resource
    private AdminProtocolService adminProtocolService;

    /** 网关配置查询应用服务 */
    @Resource
    private AdminManageService adminManageService;

    /**
     * 保存网关基础配置
     * 处理网关基本信息、鉴权开关、状态等配置
     *
     * @param requestDTO 网关配置请求参数
     * @return 统一响应结果
     */
    @PostMapping("save_gateway_config")
    @Override
    public Response<GatewayConfigResponseDTO> saveGatewayConfig(@RequestBody GatewayConfigRequestDTO.GatewayConfig requestDTO) {
        try {
            log.info("保存网关配置开始 gatewayId: {}", requestDTO.getGatewayId());

            // 构建领域层命令实体：请求DTO → VO → Command
            GatewayConfigCommandEntity commandEntity = GatewayConfigCommandEntity
                    .builder()
                    .gatewayConfigVO(GatewayConfigVO
                            .builder()
                            .gatewayId(requestDTO.getGatewayId())
                            .gatewayName(requestDTO.getGatewayName())
                            .gatewayDescription(requestDTO.getGatewayDescription())
                            .gatewayVersion(requestDTO.getGatewayVersion())
                            .auth(GatewayEnum.GatewayAuthStatusEnum.getByCode(requestDTO.getAuth()))
                            .status(GatewayEnum.GatewayStatus.get(requestDTO.getStatus()))
                            .build())
                    .build();

            // 同步调用应用服务，执行配置保存
            adminGatewayService.saveGatewayConfig(commandEntity);

            // 返回成功响应
            return Response.success(new GatewayConfigResponseDTO(true));
        } catch (Exception e) {
            log.error("保存网关配置失败 gatewayId: {}", requestDTO.getGatewayId(), e);
            return Response.fail(ResponseCode.UN_ERROR);
        }
    }

    /**
     * 保存网关工具配置
     * 处理网关绑定的工具链信息
     *
     * @param requestDTO 网关工具配置请求参数
     * @return 统一响应结果
     */
    @PostMapping("save_gateway_tool_config")
    @Override
    public Response<GatewayConfigResponseDTO> saveGatewayToolConfig(@RequestBody GatewayConfigRequestDTO.GatewayToolConfig requestDTO) {
        try {
            log.info("保存网关工具配置开始 gatewayId: {}", requestDTO.getGatewayId());

            // 构建工具配置命令实体
            GatewayToolConfigCommandEntity commandEntity = GatewayToolConfigCommandEntity
                    .builder()
                    .gatewayToolConfigVO(GatewayToolConfigVO
                            .builder()
                            .gatewayId(requestDTO.getGatewayId())
                            .toolId(requestDTO.getToolId())
                            .toolName(requestDTO.getToolName())
                            .toolType(requestDTO.getToolType())
                            .toolDescription(requestDTO.getToolDescription())
                            .toolVersion(requestDTO.getToolVersion())
                            .protocolId(requestDTO.getProtocolId())
                            .protocolType(requestDTO.getProtocolType())
                            .build())
                    .build();

            // 同步调用服务保存工具配置
            adminGatewayService.saveGatewayToolConfig(commandEntity);

            return Response.success(new GatewayConfigResponseDTO(true));
        } catch (Exception e) {
            log.error("保存网关工具配置失败 gatewayId: {}", requestDTO.getGatewayId(), e);
            return Response.fail(ResponseCode.UN_ERROR);
        }
    }

    /**
     * 保存网关协议配置
     * 处理HTTP协议、请求头、超时、字段映射规则等
     *
     * @param requestDTO 网关协议配置请求参数
     * @return 统一响应结果
     */
    @PostMapping("save_gateway_protocol")
    @Override
    public Response<GatewayConfigResponseDTO> saveGatewayProtocol(@RequestBody GatewayConfigRequestDTO.GatewayProtocol requestDTO) {
        try {
            log.info("保存网关协议配置开始，请求协议数量: {}", Optional
                    .ofNullable(requestDTO.getHttpProtocols())
                    .map(List::size)
                    .orElse(0));

            // 1. 构建协议存储命令实体
            StorageCommandEntity commandEntity = new StorageCommandEntity();

            // 2. 转换并深度校验：通过 Stream 流触发 convertHttpProtocol 内部的 Assert/Exception
            List<HTTPProtocolVO> protocolVOS = Optional
                    .ofNullable(requestDTO.getHttpProtocols())
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(this::convertHttpProtocol) // 触发对 mcpType 的校验
                    .collect(Collectors.toList());

            // 3. 校验协议列表是否为空（根据业务需求可选）
            if (protocolVOS.isEmpty()) {
                return Response.fail(ResponseCode.ILLEGAL_PARAMETER);
            }

            // 4. 设置协议列表并调用服务执行持久化（同步阻塞执行）
            commandEntity.setHttpProtocolVOS(protocolVOS);
            adminProtocolService.saveGatewayProtocol(commandEntity);

            log.info("保存网关协议配置成功");
            return Response.success(new GatewayConfigResponseDTO(true));

        } catch (IllegalArgumentException e) {
            // 捕获转换逻辑中抛出的参数缺失异常（如 mcpType 缺失）
            log.warn("保存网关协议参数校验失败: {}", e.getMessage());
            return Response.fail(ResponseCode.ILLEGAL_PARAMETER);

        } catch (Exception e) {
            // 捕获数据库约束冲突（DataIntegrityViolationException）或其他系统异常
            log.error("保存网关协议配置系统异常", e);
            return Response.fail(ResponseCode.UN_ERROR);
        }
    }

    /**
     * 保存网关认证配置（API Key注册）
     * 生成网关接入密钥、限流、过期时间等认证信息
     * 底层为同步阻塞调用，异常直接抛出并捕获
     *
     * @param requestDTO 网关认证请求参数
     * @return 统一响应结果
     */
    @PostMapping("save_gateway_auth")
    @Override
    public Response<GatewayConfigResponseDTO> saveGatewayAuth(@RequestBody GatewayConfigRequestDTO.GatewayAuth requestDTO) {
        try {
            log.info("保存网关认证配置开始 gatewayId: {}", requestDTO.getGatewayId());

            // 构建注册命令实体
            RegisterCommandEntity commandEntity = RegisterCommandEntity
                    .builder()
                    .gatewayId(requestDTO.getGatewayId())
                    .rateLimit(requestDTO.getRateLimit())
                    .expireTime(requestDTO.getExpireTime())
                    .build();

            // 同步调用认证注册服务，重复注册会抛出异常
            adminAuthService.saveGatewayAuth(commandEntity);

            log.info("保存网关认证配置成功 gatewayId: {}", requestDTO.getGatewayId());
            return Response.success(new GatewayConfigResponseDTO(true));
        } catch (Exception e) {
            log.error("保存网关认证配置失败 gatewayId: {}", requestDTO.getGatewayId(), e);
            return Response.fail(ResponseCode.UN_ERROR);
        }
    }

    /**
     * 查询网关配置列表
     * 用于管理端展示所有网关信息
     *
     * @return 网关配置列表响应
     */
    @GetMapping("query_gateway_config_list")
    @Override
    public Response<List<GatewayConfigDTO>> queryGatewayConfigList() {
        try {
            log.info("查询网关配置列表开始");

            // 调用应用服务获取领域实体列表
            List<GatewayConfigEntity> entities = adminManageService.queryGatewayConfigList();

            // 领域实体 → 前端DTO转换
            List<GatewayConfigDTO> dtoList = entities
                    .stream()
                    .map(e -> GatewayConfigDTO
                            .builder()
                            .gatewayId(e.getGatewayId())
                            .gatewayName(e.getGatewayName())
                            .gatewayDesc(e.getGatewayDescription())
                            .version(e.getGatewayVersion())
                            .auth(e.getAuth())
                            .status(e.getStatus())
                            .build())
                    .collect(Collectors.toList());

            return Response.success(dtoList);
        } catch (Exception e) {
            log.error("查询网关配置列表失败", e);
            return Response.fail(ResponseCode.UN_ERROR);
        }
    }

    /**
     * HTTP协议参数转换
     * 请求DTO → 领域层HTTPProtocolVO
     * 统一封装转换逻辑，提升代码复用性与可读性
     *
     * @param p 请求层协议对象
     * @return 领域层协议视图对象
     */
    private HTTPProtocolVO convertHttpProtocol(GatewayConfigRequestDTO.GatewayProtocol.HTTPProtocol p) {
        // 构建基础协议对象
        HTTPProtocolVO vo = HTTPProtocolVO
                .builder()
                .protocolId(p.getProtocolId())
                .httpUrl(p.getHttpUrl())
                .httpHeaders(p.getHttpHeaders())
                .httpMethod(p.getHttpMethod())
                .timeout(p.getTimeout())
                .build();

        // 空安全处理：字段映射列表转换
        List<HTTPProtocolVO.ProtocolMapping> mappingVOS = Optional
                .ofNullable(p.getMappings())
                .orElse(Collections.emptyList())
                .stream()
                .map(m -> {
                    // 校验 mcpType 是否为空
                    if (m.getMcpType() == null || m
                            .getMcpType()
                            .trim()
                            .isEmpty()) {
                        throw new IllegalArgumentException(String.format("协议配置异常: 协议ID[%s] 的字段[%s] mcpType " +
                                "缺失，数据库禁止为空", p.getProtocolId(), m.getFieldName()));
                    }
                    // 校验 mcpPath 是否为空
                    if (m.getMcpPath() == null || m
                            .getMcpPath()
                            .trim()
                            .isEmpty()) {
                        throw new IllegalArgumentException(String.format("协议配置异常: 协议ID[%s] 的字段[%s] mcpPath 缺失",
                                p.getProtocolId(), m.getFieldName()));
                    }

                    // 执行构建
                    return HTTPProtocolVO.ProtocolMapping
                            .builder()
                            .mappingType(m.getMappingType())
                            .parentPath(m.getParentPath())
                            .fieldName(m.getFieldName())
                            .mcpPath(m.getMcpPath())
                            .mcpType(m.getMcpType())
                            .mcpDescription(m.getDescription())
                            .isRequired(Optional
                                    .ofNullable(m.getIsRequired())
                                    .orElse(0))
                            .sortOrder(m.getSortOrder())
                            .build();
                })
                .collect(Collectors.toList());

        // 设置映射关系并返回
        vo.setMappings(mappingVOS);
        return vo;
    }
}