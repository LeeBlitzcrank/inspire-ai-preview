package com.inspire.platform.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inspire.platform.auth.dto.*;
import com.inspire.platform.auth.entity.LoginLog;
import com.inspire.platform.auth.entity.PasswordReset;
import com.inspire.platform.auth.entity.User;
import com.inspire.platform.auth.mapper.LoginLogMapper;
import com.inspire.platform.auth.mapper.PasswordResetMapper;
import com.inspire.platform.auth.mapper.UserMapper;
import com.inspire.platform.auth.service.AuthService;
import com.inspire.platform.auth.service.email.EmailService;
import com.inspire.platform.auth.util.JwtUtil;
import com.inspire.platform.auth.util.RedisSessionUtil;
import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.mq.constant.MqTopicConstants;
import com.inspire.platform.mq.producer.MqProducer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final LoginLogMapper loginLogMapper;
    private final UserMapper userMapper;
    private final PasswordResetMapper passwordResetMapper;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final RedisSessionUtil redisSessionUtil;
    private final MqProducer mqProducer;
    private final HttpServletRequest request;

    public AuthServiceImpl(LoginLogMapper loginLogMapper, UserMapper userMapper,
                           PasswordResetMapper passwordResetMapper, EmailService emailService,
                           JwtUtil jwtUtil, RedisSessionUtil redisSessionUtil,
                           MqProducer mqProducer, HttpServletRequest request) {
        this.loginLogMapper = loginLogMapper;
        this.userMapper = userMapper;
        this.passwordResetMapper = passwordResetMapper;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.redisSessionUtil = redisSessionUtil;
        this.mqProducer = mqProducer;
        this.request = request;
    }

    // ==================== 随机昵称/头像生成（注册用） ====================

    private static final String[] NICKNAME_ADJ = {
        "快乐","聪明","勇敢","温柔","可爱","阳光","清风","明月","星辰","大海",
        "梦幻","甜蜜","俏皮","灵动","清新","文艺","酷炫","温暖","淡雅","宁静",
        "率真","纯良","元气","欢脱","悠然","逍遥","闲适","天真","浪漫","自在"
    };
    private static final String[] NICKNAME_NOUN = {
        "小鱼","小猫","小鹿","蝴蝶","樱花","奶茶","豆浆","包子","饼干","糖果",
        "花猫","松鼠","兔子","羊羔","熊猫","布丁","甜甜","冰淇淋","棉花糖","小幸福",
        "星河","云朵","风铃","彩虹","萤火虫","向日葵","满天星","蒲公英","千纸鹤","小确幸"
    };
    private static final String[] AVATAR_LIST = {
        "🌸","🍀","🦋","🌈","⭐","🌙","☀️",
        "🍁","🌺","🐱","🐰","🦄","🐼","🦊",
        "🐨","🎨","🎵","🎯","🎮","📚",
        "🍃","🍌","🍥","🍪","🥜","🍭","🍮",
        "🎁","🎉","✨"
    };
    private static final Random RANDOM = new Random();


    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    // ==================== 注册 ====================
    // 文档 4.1：注册成功后自动创建会话，返回双Token

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次密码输入不一致");
        }
        if (findByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已被注册");
        }
        if (findByEmail(request.getEmail()) != null) {
            throw new BusinessException("该邮箱已被注册");
        }

        User user = new User();
        user.setId(generateSnowflakeId());
        user.setUsername(request.getUsername());
        user.setPassword(PASSWORD_ENCODER.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname() != null && !request.getNickname().isEmpty()
                ? request.getNickname() : generateRandomNickname());
        user.setAvatar(request.getAvatar() != null && !request.getAvatar().isEmpty()
                ? request.getAvatar() : generateRandomAvatar());
        user.setRole("user"); // 默认普通用户角色
        user.setStatus(1);    // 正常状态
        user.setCity("");
        user.setDeleted(0);

        userMapper.insert(user);

        // 注册完成后自动创建双Token会话
        TokenResponse tokenResp = createDualTokenSession(user);

        mqProducer.send(MqTopicConstants.TOPIC_USER_REGISTER,
                java.util.Map.of("userId", user.getId(), "username", user.getUsername(), "email", user.getEmail()));
        log.info("用户注册成功: userId={}, username={}, email={}", user.getId(), user.getUsername(), user.getEmail());
        return tokenResp;
    }

    // ==================== 登录（文档流程一） ====================

    @Override
    public TokenResponse login(LoginRequest request) {
        // ① 查询用户 & 校验
        User user = findByUsername(request.getUsername());
        if (user == null) {
            log.warn("登录失败: 账号不存在 username={}", request.getUsername());
            saveLoginLog(null, request.getUsername(), 0, "账号或密码错误", "password");
            throw new BusinessException("账号或密码错误");
        }

        // ② 校验账号状态（文档4.1.2 校验账号是否冻结）
        if (user.getStatus() != null && user.getStatus() == 0) {
            log.warn("登录失败: 账号已冻结 userId={}", user.getId());
            saveLoginLog(user.getId(), request.getUsername(), 0, "账号已冻结", "password");
            throw new BusinessException("账号已被冻结，请联系管理员");
        }

        // ③ 密码校验
        if (!PASSWORD_ENCODER.matches(request.getPassword(), user.getPassword())) {
            log.warn("登录失败: 密码错误 userId={}", user.getId());
            saveLoginLog(user.getId(), request.getUsername(), 0, "密码错误", "password");
            throw new BusinessException("账号或密码错误");
        }

        // ④ 创建双Token会话（含SSO挤旧逻辑 + Redis缓存入库）
        TokenResponse tokenResp = createDualTokenSession(user);

        // ⑤ 记录登录成功日志
        saveLoginLog(user.getId(), user.getUsername(), 1, "", "password");

        // ⑥ 发送MQ消息
        mqProducer.send(MqTopicConstants.TOPIC_USER_BEHAVIOR,
                java.util.Map.of("userId", user.getId(), "username", user.getUsername(), "type", "login"));
        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return tokenResp;
    }

    // ==================== 无感刷新（文档流程三） ====================

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        // ① 校验 RefreshToken 非空
        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("刷新失败: RefreshToken为空");
            throw new BusinessException(401005, "RefreshToken不存在或已过期");
        }

        // ② 查询 Redis refresh:{refreshToken}
        Long userId = redisSessionUtil.getUserIdByRefreshToken(refreshToken);
        if (userId == null) {
            log.warn("刷新失败: RefreshToken不存在/已过期 token={}", maskToken(refreshToken));
            throw new BusinessException(401005, "RefreshToken不存在或已过期");
        }

        // ③ 获取用户信息
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            log.warn("刷新失败: 用户不存在 userId={}", userId);
            redisSessionUtil.clearUserSession(userId, refreshToken);
            throw new BusinessException(401005, "用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            log.warn("刷新失败: 账号已冻结 userId={}", userId);
            redisSessionUtil.clearUserSession(userId, refreshToken);
            throw new BusinessException("账号已被冻结");
        }

        // ④ 生成全新 AccessToken（不更新 RefreshToken）
        String newAccessToken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // ⑤ 记录日志
        saveLoginLog(user.getId(), user.getUsername(), 1, "", "refresh");
        log.info("令牌刷新成功: userId={}", userId);

        // ⑥ 返回新 AccessToken + 旧 RefreshToken（重复使用，文档4.3.3第4步）
        TokenResponse resp = new TokenResponse();
        resp.setAccessToken(newAccessToken);
        resp.setRefreshToken(refreshToken);
        resp.setExpiresIn(jwtUtil.getExpirationMs() / 1000);
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setAvatar(user.getAvatar());
        resp.setRole(user.getRole());
        return resp;
    }

    // ==================== 用户登出（文档流程四） ====================

    @Override
    public void logout(String accessToken, String refreshToken) {
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("登出失败: AccessToken为空");
            return; // 无token登出视为成功
        }

        Long userId = null;

        // ① 解析 AccessToken，计算剩余时间，写入黑名单
        try {
            Claims claims = jwtUtil.parseToken(accessToken);
            userId = Long.parseLong(claims.getSubject());
            long remainingSeconds = jwtUtil.getRemainingSeconds(accessToken);

            if (remainingSeconds > 0) {
                redisSessionUtil.addToBlacklist(accessToken, remainingSeconds);
                log.info("登出: 黑名单写入, remaining={}s, userId={}", remainingSeconds, userId);
            }
        } catch (ExpiredJwtException e) {
            // 令牌已过期：无法计算TTL，但仍可清理会话
            Claims expiredClaims = e.getClaims();
            userId = expiredClaims != null ? Long.parseLong(expiredClaims.getSubject()) : null;
            log.warn("登出: AccessToken已过期, userId={}", userId);
        } catch (JwtException e) {
            log.warn("登出: AccessToken非法, msg={}", e.getMessage());
        }

        // ② 删除 refresh 会话缓存（文档4.4.2第3步）
        if (refreshToken != null && !refreshToken.isBlank()) {
            redisSessionUtil.deleteRefreshToken(refreshToken);
        }
        if (userId != null) {
            redisSessionUtil.deleteUserRefreshMapping(userId);
        }

        log.info("用户登出成功: userId={}", userId);
    }

    // ==================== 后台强制下线（文档流程五） ====================

    @Override
    public void kickUser(Long adminUserId, Long targetUserId) {
        if (targetUserId == null) {
            throw new BusinessException("被踢用户ID不能为空");
        }

        log.info("管理员踢人: adminUserId={}, targetUserId={}", adminUserId, targetUserId);

        // ① 通过 userId 查询绑定的 RefreshToken（文档4.5.2第2步）
        String refreshToken = redisSessionUtil.getUserRefreshToken(targetUserId);

        // ② 删除该用户全部会话缓存
        if (refreshToken != null) {
            redisSessionUtil.deleteRefreshToken(refreshToken);
        }
        redisSessionUtil.deleteUserRefreshMapping(targetUserId);

        log.info("管理员踢人成功: adminUserId={}, targetUserId={}, refreshToken已删除",
                adminUserId, targetUserId);
    }

    // ==================== 个人信息 ====================

    @Override
    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    @Override
    public User updateUserInfo(Long userId, UserUpdateRequest request) {
        User user = getUserById(userId);
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname().isEmpty() ? user.getUsername() : request.getNickname());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        userMapper.updateById(user);
        log.info("用户信息更新: userId={}", userId);
        return user;
    }

    // ==================== 修改密码 ====================

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUserById(userId);
        if (!PASSWORD_ENCODER.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码不正确");
        }
        user.setPassword(PASSWORD_ENCODER.encode(request.getNewPassword()));
        userMapper.updateById(user);
        log.info("密码修改成功: userId={}", userId);
    }

    // ==================== 忘记密码（保持不变） ====================

    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = findByEmail(email);
        if (user == null) {
            throw new BusinessException("该邮箱未注册");
        }

        // 生成UUID令牌，30分钟过期
        String token = UUID.randomUUID().toString().replace("-", "");
        PasswordReset record = new PasswordReset();
        record.setId(generateSnowflakeId());
        record.setUserId(user.getId());
        record.setEmail(email);
        record.setToken(token);
        record.setExpiryTime(LocalDateTime.now().plusMinutes(30));
        record.setUsed(0);

        passwordResetMapper.insert(record);

        // 发送邮件（SMTP或控制台兜底）
        emailService.sendPasswordResetEmail(email, token);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        LambdaQueryWrapper<PasswordReset> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PasswordReset::getToken, token);
        wrapper.eq(PasswordReset::getUsed, 0);
        PasswordReset record = passwordResetMapper.selectOne(wrapper);

        if (record == null) {
            throw new BusinessException("重置令牌无效或已使用");
        }
        if (record.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("重置令牌已过期，请重新申请");
        }

        // 更新密码
        User user = getUserById(record.getUserId());
        user.setPassword(PASSWORD_ENCODER.encode(newPassword));
        userMapper.updateById(user);

        // 标记令牌已使用
        record.setUsed(1);
        passwordResetMapper.updateById(record);

        log.info("密码重置成功: userId={}", user.getId());
    }

    // ==================== 查询工具 ====================

    private User findByUsername(String username) {

        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getUsername, username);
        wrapper.eq(User::getDeleted, 0);
        return userMapper.selectOne(wrapper);
    }

    private User findByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getEmail, email);
        wrapper.eq(User::getDeleted, 0);
        return userMapper.selectOne(wrapper);
    }

    // ==================== 双Token会话创建（登录/注册复用） ====================

    /**
     * 创建双Token会话（文档流程一 第5-7步）
     * <p>
     * 1. 生成 AccessToken（JWT，15分钟）
     * 2. 生成 32位随机字符串 RefreshToken
     * 3. SSO：若该用户已有登录会话，删除旧 RefreshToken 缓存
     * 4. 写入 refresh:{refreshToken} → userId（7天）
     * 5. 写入 user_refresh:{userId} → refreshToken（7天）
     * 6. 返回 TokenResponse
     */
    private TokenResponse createDualTokenSession(User user) {
        // ① 生成 AccessToken
        String accessToken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // ② 生成 32位随机字符串 RefreshToken
        String refreshToken = generateRefreshToken();

        // ③ 单点登录：挤掉旧设备（文档4.1.2第6步）
        redisSessionUtil.invalidateOldSession(user.getId());

        // ④ 写入 RefreshToken 会话缓存
        redisSessionUtil.saveRefreshToken(refreshToken, user.getId());

        // ⑤ 写入用户 → RefreshToken 映射绑定
        redisSessionUtil.saveUserRefreshMapping(user.getId(), refreshToken);

        // ⑥ 构建返回结果
        TokenResponse resp = new TokenResponse();
        resp.setAccessToken(accessToken);
        resp.setRefreshToken(refreshToken);
        resp.setExpiresIn(jwtUtil.getExpirationMs() / 1000);
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setAvatar(user.getAvatar());
        resp.setRole(user.getRole());
        return resp;
    }

    // ==================== 登录日志 ====================

    /**
     * 插入登录日志（文档6.2节：记录IP、设备、时间、操作结果）
     */
    private void saveLoginLog(Long userId, String username, int result, String failReason, String loginType) {
        try {
            String ip = extractClientIp();
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null && userAgent.length() > 500) {
                userAgent = userAgent.substring(0, 500);
            }

            LoginLog logEntry = new LoginLog();
            logEntry.setId(generateSnowflakeId());
            logEntry.setUserId(userId != null ? userId : 0L);
            logEntry.setUsername(username != null ? username : "");
            logEntry.setIp(ip != null ? ip : "");
            logEntry.setDeviceId("");
            logEntry.setUserAgent(userAgent != null ? userAgent : "");
            logEntry.setLoginType(loginType);
            logEntry.setResult(result);
            logEntry.setFailReason(failReason != null ? failReason : "");

            loginLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("登录日志写入失败: {}", e.getMessage());
        }
    }

    /**
     * 提取客户端真实IP
     */
    private String extractClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    // ==================== 通用工具 ====================

    /**
     * 生成 32 位随机字符串作为 RefreshToken
     * 文档 4.1.2 节第4步：32位UUID随机字符串
     */
    private String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    private String generateRandomNickname() {
        return NICKNAME_ADJ[RANDOM.nextInt(NICKNAME_ADJ.length)]
             + NICKNAME_NOUN[RANDOM.nextInt(NICKNAME_NOUN.length)];
    }

    private String generateRandomAvatar() {
        return AVATAR_LIST[RANDOM.nextInt(AVATAR_LIST.length)];
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 30) {
            return token;
        }
        return token.substring(0, 15) + "..." + token.substring(token.length() - 10);
    }

    // ==================== 雪花ID生成器 ====================

    private static long snowflakeLastTimestamp = -1L;
    private static long snowflakeSequence = 0L;
    private static final long SNOWFLAKE_WORKER_ID = 1L;
    private static final long SNOWFLAKE_DATACENTER_ID = 1L;

    private static synchronized long generateSnowflakeId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < snowflakeLastTimestamp) {
            timestamp = snowflakeLastTimestamp;
        }
        if (timestamp == snowflakeLastTimestamp) {
            snowflakeSequence = (snowflakeSequence + 1) & 0xFFF;
            if (snowflakeSequence == 0) {
                timestamp++;
            }
        } else {
            snowflakeSequence = 0;
        }
        snowflakeLastTimestamp = timestamp;
        return ((timestamp - 1735689600000L) << 22)
                | (SNOWFLAKE_DATACENTER_ID << 12)
                | SNOWFLAKE_WORKER_ID;
    }

    // ==================== 校验工具 ====================
}
