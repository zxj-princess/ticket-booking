package com.ticket.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.entity.User;

public interface UserService {

    User register(User user);

    String login(String username, String password);

    User getUserById(Long id);

    User getUserByUsername(String username);

    void updateUser(User user);

    Page<User> listUsers(int page, int size, String keyword);

    void updateStatus(Long id, Integer status);

    void deleteUser(Long id);
}
