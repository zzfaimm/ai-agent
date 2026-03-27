package com.iverson.aiagent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户管理接口 - 用于测试 Knife4j 文档生成
 */
@Tag(name = "用户管理", description = "提供用户的增删改查接口")
@RestController
@RequestMapping("/api/users")
public class UserController {

    // 模拟数据库存储（仅用于测试）
    private final ConcurrentHashMap<Long, User> userDB = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Operation(summary = "创建用户", description = "传入用户信息，返回创建后的用户对象")
    @PostMapping
    public User createUser(@RequestBody User user) {
        long id = idGenerator.getAndIncrement();
        user.setId(id);
        userDB.put(id, user);
        return user;
    }

    @Operation(summary = "获取用户列表", description = "返回所有用户")
    @GetMapping
    public List<User> listUsers() {
        return new ArrayList<>(userDB.values());
    }

    @Operation(summary = "根据ID查询用户", description = "路径参数传入用户ID，返回对应用户")
    @GetMapping("/{id}")
    public User getUserById(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long id) {
        return userDB.get(id);
    }

    @Operation(summary = "更新用户信息", description = "传入用户ID和新数据，更新后返回完整用户对象")
    @PutMapping("/{id}")
    public User updateUser(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id,
            @RequestBody User user) {
        if (!userDB.containsKey(id)) {
            throw new RuntimeException("用户不存在");
        }
        user.setId(id);
        userDB.put(id, user);
        return user;
    }

    @Operation(summary = "删除用户", description = "根据ID删除用户")
    @DeleteMapping("/{id}")
    public String deleteUser(
            @Parameter(description = "用户ID", required = true) @PathVariable Long id) {
        userDB.remove(id);
        return "删除成功";
    }

    // 内部用户实体类（也可以单独放在model包）
    @Data
    public static class User {
        private Long id;
        private String name;
        private Integer age;
        private String email;
    }
}