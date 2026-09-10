package com.defect.platform.vo;

import com.defect.platform.common.constant.RoleEnum;
import com.defect.platform.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息视图对象（不含密码等敏感字段）
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    /** 角色编码 */
    private String role;

    /** 角色中文描述 */
    private String roleDesc;

    /** 状态：1-启用 0-禁用 */
    private Integer status;

    private String avatar;

    private LocalDateTime createTime;

    /**
     * 实体转视图对象（隐藏密码字段）
     */
    public static UserVO from(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setRoleDesc(roleDesc(user.getRole()));
        vo.setStatus(user.getStatus());
        vo.setAvatar(user.getAvatar());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    private static String roleDesc(String role) {
        for (RoleEnum r : RoleEnum.values()) {
            if (r.getCode().equals(role)) {
                return r.getDesc();
            }
        }
        return role;
    }
}
