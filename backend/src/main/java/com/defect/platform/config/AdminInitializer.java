package com.defect.platform.config;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.defect.platform.common.constant.RoleEnum;
import com.defect.platform.entity.User;
import com.defect.platform.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 管理员账号初始化：应用首次启动时自动创建默认管理员（账号密码见 application.yml）
 */
@Slf4j
@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserMapper userMapper;

    private final String username;
    private final String password;
    private final String nickname;

    public AdminInitializer(UserMapper userMapper,
                            @Value("${init.admin.username}") String username,
                            @Value("${init.admin.password}") String password,
                            @Value("${init.admin.nickname}") String nickname) {
        this.userMapper = userMapper;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
    }

    @Override
    public void run(String... args) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count != null && count > 0) {
            log.info("管理员账号已存在，跳过初始化: {}", username);
            return;
        }

        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(BCrypt.hashpw(password));
        admin.setNickname(nickname);
        admin.setRole(RoleEnum.ADMIN.getCode());
        admin.setStatus(1);
        userMapper.insert(admin);
        log.info("已初始化管理员账号: {}", username);
    }
}
