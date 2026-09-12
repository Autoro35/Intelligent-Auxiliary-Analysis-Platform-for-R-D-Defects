package com.defect.platform.controller;

import com.defect.platform.common.Result;
import com.defect.platform.common.annotation.RateLimit;
import com.defect.platform.dto.LoginDTO;
import com.defect.platform.dto.RegisterDTO;
import com.defect.platform.service.UserService;
import com.defect.platform.vo.LoginVO;
import com.defect.platform.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：注册、登录、当前用户信息
 * <p>注册接口按规格保留（对外可用），但前端登录页不再暴露入口——
 * 新账号一律是访客角色且无法自助升级，实际应由管理员在「用户管理」中分配。</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户注册（公开）
     * <p>开号成本低但会被滥用，按 IP 收紧到 5 次/5 分钟</p>
     */
    @PostMapping("/register")
    @RateLimit(limit = 5, window = 300, dimension = RateLimit.Dimension.IP,
            message = "注册过于频繁，请稍后再试")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success("注册成功", userService.register(dto));
    }

    /**
     * 用户登录（公开），返回 token
     * <p>按 IP 限制尝试次数，降低撞库风险</p>
     */
    @PostMapping("/login")
    @RateLimit(limit = 10, window = 60, dimension = RateLimit.Dimension.IP,
            message = "登录尝试过于频繁，请稍后再试")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success("登录成功", userService.login(dto));
    }

    /**
     * 获取当前登录用户信息（需登录）
     */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(userService.getCurrentUser());
    }
}
