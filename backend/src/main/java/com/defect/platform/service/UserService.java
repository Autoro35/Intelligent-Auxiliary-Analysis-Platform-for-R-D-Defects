package com.defect.platform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.defect.platform.dto.LoginDTO;
import com.defect.platform.dto.RegisterDTO;
import com.defect.platform.entity.User;
import com.defect.platform.vo.LoginVO;
import com.defect.platform.vo.UserVO;

/**
 * 用户服务：注册、登录、当前用户信息
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
}
