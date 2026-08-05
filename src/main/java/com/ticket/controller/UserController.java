package com.ticket.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.common.result.R;
import com.ticket.dto.LoginDTO;
import com.ticket.dto.RegisterDTO;
import com.ticket.entity.AiChatRecord;
import com.ticket.entity.Passenger;
import com.ticket.entity.User;
import com.ticket.mapper.AiChatRecordMapper;
import com.ticket.mapper.PassengerMapper;
import com.ticket.service.UserService;
import com.ticket.vo.LoginVO;
import com.ticket.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PassengerMapper passengerMapper;

    @Autowired
    private AiChatRecordMapper aiChatRecordMapper;

    @PostMapping("/register")
    public R<UserVO> register(@RequestBody RegisterDTO dto) {
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        User registered = userService.register(user);
        return R.success(convertToVO(registered));
    }

    @PostMapping("/login")
    public R<LoginVO> login(@RequestBody LoginDTO dto) {
        String token = userService.login(dto.getUsername(), dto.getPassword());
        User user = userService.getUserByUsername(dto.getUsername());
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUser(convertToVO(user));
        return R.success(vo);
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        // 从Authorization header中获取token并解析userId
        String token = request.getHeader("Authorization");
        Long userId = null;
        if (token != null && !token.isEmpty()) {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            if (token.contains("_")) {
                try {
                    userId = Long.parseLong(token.split("_")[0]);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (userId != null) {
            // 退出登录时清空AI聊天记录和乘客信息
            LambdaUpdateWrapper<AiChatRecord> chatWrapper = new LambdaUpdateWrapper<>();
            chatWrapper.eq(AiChatRecord::getUserId, userId)
                    .set(AiChatRecord::getDeleted, 1);
            aiChatRecordMapper.update(null, chatWrapper);

            LambdaUpdateWrapper<Passenger> passengerWrapper = new LambdaUpdateWrapper<>();
            passengerWrapper.eq(Passenger::getUserId, userId)
                    .set(Passenger::getDeleted, 1);
            passengerMapper.update(null, passengerWrapper);
        }
        return R.success();
    }

    @GetMapping("/info")
    public R<UserVO> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        return R.success(convertToVO(user));
    }

    @GetMapping("/wallet")
    public R<java.util.Map<String, Object>> getWalletBalance(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        java.math.BigDecimal balance = user.getBalance() != null ? user.getBalance() : java.math.BigDecimal.ZERO;
        return R.success(java.util.Map.of("balance", balance));
    }

    @PutMapping("/update")
    public R<UserVO> updateUser(@RequestBody User user) {
        userService.updateUser(user);
        User updated = userService.getUserById(user.getId());
        return R.success(convertToVO(updated));
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/list")
    public R<Object> listUsers(@RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) String keyword) {
        Page<User> userPage = userService.listUsers(page, size, keyword);
        IPage<UserVO> voPage = userPage.convert(this::convertToVO);
        return R.success((Object) voPage);
    }

    @PutMapping("/status")
    public R<Void> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return R.success();
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return R.success();
    }

    @GetMapping("/passengers")
    public R<List<Passenger>> listPassengers(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Passenger> passengers = passengerMapper.selectList(
            new LambdaQueryWrapper<Passenger>()
                .eq(Passenger::getUserId, userId)
                .eq(Passenger::getDeleted, 0)
        );
        return R.success(passengers);
    }

    @PostMapping("/passengers")
    public R<Passenger> addPassenger(@RequestBody Passenger passenger, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        passenger.setUserId(userId);
        passenger.setDeleted(0);
        passengerMapper.insert(passenger);
        return R.success(passenger);
    }

    @PutMapping("/passengers/{id}")
    public R<Passenger> updatePassenger(@PathVariable Long id, @RequestBody Passenger passenger, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Passenger existing = passengerMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return R.fail(403, "无权操作");
        }
        passenger.setId(id);
        passenger.setUserId(userId);
        passengerMapper.updateById(passenger);
        return R.success(passenger);
    }

    @DeleteMapping("/passengers/{id}")
    public R<Void> deletePassenger(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Passenger existing = passengerMapper.selectById(id);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return R.fail(403, "无权操作");
        }
        passengerMapper.deleteById(id);
        return R.success();
    }

    private Long getUserIdFromToken(String token) {
        if (token == null || !token.contains("_")) {
            return null;
        }
        try {
            return Long.parseLong(token.split("_")[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
