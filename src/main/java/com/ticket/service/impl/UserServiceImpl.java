package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.common.exception.BusinessException;
import com.ticket.entity.AiChatRecord;
import com.ticket.entity.Order;
import com.ticket.entity.Passenger;
import com.ticket.entity.User;
import com.ticket.mapper.AiChatRecordMapper;
import com.ticket.mapper.OrderMapper;
import com.ticket.mapper.PassengerMapper;
import com.ticket.mapper.UserMapper;
import com.ticket.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private AiChatRecordMapper aiChatRecordMapper;

    @Autowired
    private PassengerMapper passengerMapper;

    @Autowired
    private com.ticket.config.SessionService sessions;

    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwords = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

    @Override
    public User register(User user) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        Long count = userMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        user.setPassword(passwords.encode(user.getPassword()));
        user.setStatus(1);
        user.setRole(0);
        userMapper.insert(user);

        return user;
    }

    @Override
    public String login(String username, String password) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        boolean legacy = user.getPassword().matches("[a-fA-F0-9]{32}");
        boolean valid = legacy ? md5Encrypt(password).equalsIgnoreCase(user.getPassword()) : passwords.matches(password, user.getPassword());
        if (!valid) {
            throw new BusinessException("密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        if (legacy) {
            user.setPassword(passwords.encode(password));
            userMapper.updateById(user);
        }
        return generateToken(user);
    }

    @Override
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    @Override
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    @Override
    public void updateUser(User user) {
        User exist = userMapper.selectById(user.getId());
        if (exist == null) {
            throw new BusinessException("用户不存在");
        }
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwords.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        userMapper.updateById(user);
    }

    @Override
    public Page<User> listUsers(int page, int size, String keyword) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getPhone, keyword));
        }
        wrapper.orderByDesc(User::getCreateTime);
        return userMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        userMapper.deleteById(id);
    }

    private String md5Encrypt(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BusinessException("密码加密失败");
        }
    }

    private String generateToken(User user) {
        return sessions.create(user.getId());
    }
}
