package com.defect.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.defect.platform.common.PageResult;
import com.defect.platform.common.ResultCode;
import com.defect.platform.common.constant.RoleEnum;
import com.defect.platform.common.context.UserContext;
import com.defect.platform.common.exception.BusinessException;
import com.defect.platform.dto.LoginDTO;
import com.defect.platform.dto.RegisterDTO;
import com.defect.platform.dto.UserUpdateDTO;
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

    // ---- 管理员维护用户 ----

    @Override
    public PageResult<UserVO> list(long current, long size, String keyword, String role, Integer status) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
        }
        wrapper.eq(StrUtil.isNotBlank(role), User::getRole, role)
                .eq(status != null, User::getStatus, status)
                .orderByDesc(User::getId);
        Page<User> page = page(new Page<>(current, size), wrapper);
        return PageResult.of(page, UserVO::from);
    }

    @Override
    public UserVO update(Long id, UserUpdateDTO dto) {
        User user = requireUser(id);

        String newRole = StrUtil.isBlank(dto.getRole()) ? null : dto.getRole().toUpperCase();
        if (newRole != null && !isValidRole(newRole)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "非法的角色值");
        }
        Integer newStatus = dto.getStatus();
        if (newStatus != null && newStatus != 1 && newStatus != 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "状态只能为 1(启用) 或 0(禁用)");
        }

        // 先判「最后一名管理员」：这是更具体的拒绝理由，也保证该约束在接口层可达
        if (isDemotingAdmin(user, newRole, newStatus)) {
            assertOtherActiveAdminExists(id);
        }
        // 再判「不能操作自己」：改自己的角色或状态容易把自己锁死在系统外
        boolean self = id.equals(UserContext.getUserId());
        boolean roleChanged = newRole != null && !newRole.equals(user.getRole());
        boolean statusChanged = newStatus != null && !newStatus.equals(user.getStatus());
        if (self && (roleChanged || statusChanged)) {
            throw new BusinessException(ResultCode.USER_SELF_OPERATION);
        }

        // updateById 默认跳过 null 字段，因此未传的字段保持原值，不会被清空
        User update = new User();
        update.setId(id);
        update.setNickname(dto.getNickname());
        update.setEmail(dto.getEmail());
        update.setRole(newRole);
        update.setStatus(newStatus);
        updateById(update);
        log.info("管理员更新用户: id={}, role={}, status={}", id, newRole, newStatus);
        return UserVO.from(getById(id));
    }

    @Override
    public void resetPassword(Long id, String password) {
        requireUser(id);
        User update = new User();
        update.setId(id);
        update.setPassword(BCrypt.hashpw(password));
        updateById(update);
        log.info("管理员重置用户密码: id={}", id);
    }

    @Override
    public void deleteUser(Long id) {
        User user = requireUser(id);
        // 与 update 保持一致的判定顺序：先「最后一名管理员」，再「不能操作自己」
        if (RoleEnum.ADMIN.getCode().equals(user.getRole()) && isActive(user)) {
            assertOtherActiveAdminExists(id);
        }
        if (id.equals(UserContext.getUserId())) {
            throw new BusinessException(ResultCode.USER_SELF_OPERATION);
        }
        removeById(id);
        log.info("管理员删除用户: id={}, username={}", id, user.getUsername());
    }

    // ---- 私有 ----

    private User requireUser(Long id) {
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    /** 该操作是否会把一名启用中的管理员降级或停用 */
    private boolean isDemotingAdmin(User user, String newRole, Integer newStatus) {
        if (!RoleEnum.ADMIN.getCode().equals(user.getRole()) || !isActive(user)) {
            return false;
        }
        boolean roleDown = newRole != null && !RoleEnum.ADMIN.getCode().equals(newRole);
        boolean statusDown = newStatus != null && newStatus == 0;
        return roleDown || statusDown;
    }

    /** 除 excludeId 外，系统是否还存在其他启用状态的管理员 */
    private void assertOtherActiveAdminExists(Long excludeId) {
        long others = count(new LambdaQueryWrapper<User>()
                .eq(User::getRole, RoleEnum.ADMIN.getCode())
                .eq(User::getStatus, 1)
                .ne(User::getId, excludeId));
        if (others == 0) {
            throw new BusinessException(ResultCode.LAST_ADMIN_PROTECTED);
        }
    }

    private boolean isActive(User user) {
        return user.getStatus() == null || user.getStatus() == 1;
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
