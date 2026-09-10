package com.defect.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.defect.platform.common.PageResult;
import com.defect.platform.common.Result;
import com.defect.platform.common.annotation.RequireRole;
import com.defect.platform.common.constant.RoleEnum;
import com.defect.platform.entity.User;
import com.defect.platform.service.UserService;
import com.defect.platform.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口（管理员）
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 分页查询用户列表（仅管理员）
     */
    @GetMapping
    @RequireRole(RoleEnum.ADMIN)
    public Result<PageResult<UserVO>> list(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size) {
        Page<User> page = userService.page(new Page<>(current, size),
                new LambdaQueryWrapper<User>().orderByDesc(User::getId));
        return Result.success(PageResult.of(page, UserVO::from));
    }
}
