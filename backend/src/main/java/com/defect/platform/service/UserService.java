package com.defect.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.defect.platform.common.PageResult;
import com.defect.platform.dto.LoginDTO;
import com.defect.platform.dto.RegisterDTO;
import com.defect.platform.dto.UserUpdateDTO;
import com.defect.platform.entity.User;
import com.defect.platform.vo.LoginVO;
import com.defect.platform.vo.UserVO;

/**
 * 用户服务：注册、登录、当前用户信息、管理员维护用户
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    UserVO register(RegisterDTO dto);

    /**
     * 用户登录，返回 token 与用户信息
     */
    LoginVO login(LoginDTO dto);

    /**
     * 获取当前登录用户信息
     */
    UserVO getCurrentUser();

    /**
     * 用户分页列表（管理员），支持按用户名/昵称关键字、角色、状态过滤
     */
    PageResult<UserVO> list(long current, long size, String keyword, String role, Integer status);

    /**
     * 更新用户信息（管理员），可改昵称/邮箱/角色/状态
     */
    UserVO update(Long id, UserUpdateDTO dto);

    /**
     * 重置用户密码（管理员）
     */
    void resetPassword(Long id, String password);

    /**
     * 删除用户（管理员，逻辑删除）
     */
    void deleteUser(Long id);
}
