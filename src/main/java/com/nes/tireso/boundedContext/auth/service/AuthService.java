package com.nes.tireso.boundedContext.auth.service;

import com.nes.tireso.base.exception.BadRequestException;
import com.nes.tireso.base.security.SecurityUtil;
import com.nes.tireso.boundedContext.auth.dto.SignInResponse;
import com.nes.tireso.boundedContext.auth.dto.TokenRequest;
import com.nes.tireso.boundedContext.auth.dto.TokenResponse;
import com.nes.tireso.boundedContext.auth.enumType.AuthProvider;
import com.nes.tireso.boundedContext.member.repository.MemberRepository;
import com.nes.tireso.boundedContext.socialLogin.service.GoogleRequestService;
import com.nes.tireso.boundedContext.socialLogin.service.KakaoRequestService;
import com.nes.tireso.boundedContext.socialLogin.service.NaverRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final KakaoRequestService kakaoRequestService;
    private final NaverRequestService naverRequestService;
    private final GoogleRequestService googleRequestService;
    private final MemberRepository memberRepository;
    private final SecurityUtil securityUtil;

    public SignInResponse redirect(TokenRequest tokenRequest) {
        if (AuthProvider.KAKAO.getAuthProvider().equals(tokenRequest.getRegistrationId())) {
            return kakaoRequestService.redirect(tokenRequest);
        } else if (AuthProvider.NAVER.getAuthProvider().equals(tokenRequest.getRegistrationId())) {
            return naverRequestService.redirect(tokenRequest);
        } else if (AuthProvider.GOOGLE.getAuthProvider().equals(tokenRequest.getRegistrationId())) {
            return googleRequestService.redirect(tokenRequest);
        }

        throw new BadRequestException("not supported oauth provider");
    }

    public SignInResponse refreshToken(TokenRequest tokenRequest) {
        String userId = (String) securityUtil.get(tokenRequest.getRefreshToken()).get("userId");
        String provider = (String) securityUtil.get(tokenRequest.getRefreshToken()).get("provider");
        String oldRefreshToken = (String) securityUtil.get(tokenRequest.getRefreshToken()).get("refreshToken");

        if (!memberRepository.existsByIdAndAuthProvider(userId, AuthProvider.findByCode(provider))) {
            throw new BadRequestException("CANNOT_FOUND_USER");
        }

        TokenResponse tokenResponse = null;
        if (AuthProvider.KAKAO.getAuthProvider().equals(provider.toLowerCase())) {
            tokenResponse = kakaoRequestService.getRefreshToken(provider, oldRefreshToken);
        } else if (AuthProvider.NAVER.getAuthProvider().equals(provider.toLowerCase())) {
            tokenResponse = naverRequestService.getRefreshToken(provider, oldRefreshToken);
        } else if (AuthProvider.GOOGLE.getAuthProvider().equals(provider.toLowerCase())) {
            tokenResponse = googleRequestService.getRefreshToken(provider, oldRefreshToken);
        }

        String accessToken = securityUtil.createAccessToken(
                userId, AuthProvider.findByCode(provider.toLowerCase()), tokenResponse.getAccessToken());

        return SignInResponse.builder()
                .authProvider(AuthProvider.findByCode(provider.toLowerCase()))
                .kakaoUserInfo(null)
                .accessToken(accessToken)
                .refreshToken(null)
                .build();
    }
}