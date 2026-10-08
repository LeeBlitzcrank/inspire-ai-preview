package com.inspire.platform.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.inspire.platform.auth.dto.SmsCodeVerifyRequest;
import com.inspire.platform.auth.dto.SmsSendRequest;
import com.inspire.platform.auth.dto.SmsSendResponse;
import com.inspire.platform.auth.dto.TokenResponse;
import com.inspire.platform.auth.entity.User;
import com.inspire.platform.auth.mapper.UserMapper;
import com.inspire.platform.common.exception.BusinessException;
import com.inspire.platform.common.validation.InputValidation;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SmsAuthService {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final UserMapper userMapper;
    private final SmsCodeService smsCodeService;
    private final AuthService authService;

    public SmsSendResponse sendCode(SmsSendRequest request) {
        String purpose = request.getPurpose().trim().toLowerCase();
        return smsCodeService.send(request.getPhone(), purpose);
    }

    @Transactional
    public TokenResponse loginOrRegister(SmsCodeVerifyRequest request) {
        String phone = InputValidation.normalizePhone(request.getPhone());
        smsCodeService.verify(phone, "login", request.getCode());

        User user = findByPhone(phone);
        if (user == null) {
            user = createPhoneUser(phone);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被冻结，请联系管理员");
        }
        return authService.issueTokens(user);
    }

    @Transactional
    public void bindPhone(Long userId, SmsCodeVerifyRequest request) {
        InputValidation.requirePositive(userId, "用户");
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(404, "用户不存在");
        }
        String phone = InputValidation.normalizePhone(request.getPhone());
        smsCodeService.verify(phone, "bind", request.getCode());
        User bound = findByPhone(phone);
        if (bound != null && !bound.getId().equals(userId)) {
            throw new BusinessException(409, "该手机号已绑定其他账号");
        }
        user.setPhone(phone);
        user.setPhoneVerified(1);
        userMapper.updateById(user);
    }

    private User findByPhone(String phone) {
        return userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getPhone, phone)
                .eq(User::getDeleted, 0)
                .last("LIMIT 1"));
    }

    private User createPhoneUser(String phone) {
        User user = new User();
        user.setId(com.inspire.platform.auth.service.impl.AuthServiceImpl.generateSnowflakeId());
        user.setUsername("u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        user.setPassword(PASSWORD_ENCODER.encode(UUID.randomUUID() + "-" + UUID.randomUUID()));
        user.setEmail(null);
        user.setPhone(phone);
        user.setPhoneVerified(1);
        user.setNickname(generateNickname(phone));
        user.setAvatar("📱");
        user.setRole("user");
        user.setCity("");
        user.setStatus(1);
        user.setDeleted(0);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            User existing = findByPhone(phone);
            if (existing != null) return existing;
            throw new BusinessException(409, "手机号注册冲突，请重试");
        }
        return user;
    }

    private String generateNickname(String phone) {
        String suffix = phone.substring(phone.length() - 4);
        for (int i = 0; i < 10; i++) {
            String candidate = "手机用户" + suffix + String.format("%02d", (int) (Math.random() * 100));
            Long count = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                    .eq(User::getNickname, candidate)
                    .eq(User::getDeleted, 0));
            if (count == null || count == 0) return candidate;
        }
        throw new BusinessException(500, "昵称生成失败，请重试");
    }
}
