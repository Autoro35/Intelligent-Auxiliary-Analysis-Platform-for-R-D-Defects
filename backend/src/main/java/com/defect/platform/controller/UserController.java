package com.defect.platform.controller;

import com.defect.platform.common.PageResult;
import com.defect.platform.common.Result;
import com.defect.platform.common.annotation.RequireRole;
import com.defect.platform.common.constant.RoleEnum;
import com.defect.platform.dto.UserPasswordDTO;
import com.defect.platform.dto.UserUpdateDTO;
import com.defect.platform.service.UserService;
import com.defect.platform.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口
 * <p>全部接口仅管理员可用；用户注册入口在 /api/auth/register，新账号一律为访客角色，
 * 由管理员在本模块调整角色后才能提单。</p>
 * <p>安全约束：不能对自己改角色/改状态/删除；系统至少保留一名启用状态的管理员。</p>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @RequireRole(RoleEnum.ADMIN)
    public Result<PageResult<UserVO>> list(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String role,
                                           @RequestParam(required = false) Integer status) {
        return Result.success(userService.list(current, size, keyword, role, status));
    }

    @PutMapping("/{id}")
    @RequireRole(RoleEnum.ADMIN)
    public Result<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        return Result.success("更新成功", userService.update(id, dto));
    }

    @PutMapping("/{id}/password")
    @RequireRole(RoleEnum.ADMIN)
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody UserPasswordDTO dto) {
        userService.resetPassword(id, dto.getPassword());
        return Result.success("密码已重置", null);
    }

    @DeleteMapping("/{id}")
    @RequireRole(RoleEnum.ADMIN)
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }
}
