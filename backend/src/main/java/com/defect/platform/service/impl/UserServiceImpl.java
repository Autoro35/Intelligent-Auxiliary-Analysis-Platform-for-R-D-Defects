package com.defect.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.defect.platform.common.ResultCode;
import com.defect.platform.common.constant.RoleEnum;
import com.defect.platform.common.context.UserContext;
import com.defect.platform.common.exception.BusinessException;
import com.defect.platform.dto.LoginDTO;
import com.defect.platform.dto.RegisterDTO;
import com.defect.platform.entity.User;
import com.defect.platform.mapper.UserMapper;
import com.defect.platform.service.UserService;
import com.defect.platform.utils.JwtUtil;
import com.defect.platform.vo.LoginVO;
import com.defect.platform.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现：密码采用 BCrypt 加密，登录签发 JWT
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtUtil jwtUtil;

    @Override
    public UserVO register(RegisterDTO dto) {
        // 用户名唯一性校验
        long exists = count(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (exists > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        // 角色校验，缺省为访客
        String role = StrUtil.isBlank(dto.getRole()) ? RoleEnum.GUEST.getCode() : dto.getRole().toUpperCase();
        if (!isValidRole(role)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "非法的角色值");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setNickname(StrUtil.blankToDefault(dto.getNickname(), dto.getUsername()));
        user.setEmail(dto.getEmail());
        user.setRole(role);
        user.setStatus(1);
        save(user);

        log.info("新用户注册: id={}, username={}, role={}", user.getId(), user.getUsername(), role);
        return UserVO.from(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()), false);
        // 用户不存在或密码错误统一提示，避免用户名枚举
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUser(UserVO.from(user));
        return vo;
    }

    @Override
    public UserVO getCurrentUser() {
        Long userId = UserContext.getUserId();
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return UserVO.from(user);
    }

    private boolean isValidRole(String role) {
        for (RoleEnum r : RoleEnum.values()) {
            if (r.getCode().equals(role)) {
                return true;
            }
        }
        return false;
    }
}
