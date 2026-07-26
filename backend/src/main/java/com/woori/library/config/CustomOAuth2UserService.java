package com.woori.library.config;

import com.woori.library.domain.AppUser;
import com.woori.library.domain.FamilyMember;
import com.woori.library.domain.NotificationSetting;
import com.woori.library.repository.AppUserRepository;
import com.woori.library.repository.FamilyMemberRepository;
import com.woori.library.repository.NotificationSettingRepository;
import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * google/naver 각각 다른 사용자 정보 응답 구조를 provider_user_id/name으로 정규화하고,
 * 처음 로그인하는 사용자는 app_user + 본인 family_member(is_self=true) + 기본 알림 설정을 함께 생성한다.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AppUserRepository appUserRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final NotificationSettingRepository notificationSettingRepository;

    public CustomOAuth2UserService(
        AppUserRepository appUserRepository,
        FamilyMemberRepository familyMemberRepository,
        NotificationSettingRepository notificationSettingRepository) {
        this.appUserRepository = appUserRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.notificationSettingRepository = notificationSettingRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User delegate = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = delegate.getAttributes();

        ProviderIdentity identity = extractIdentity(registrationId, attributes);

        AppUser appUser = appUserRepository
            .findByProviderAndProviderUserId(registrationId, identity.providerUserId())
            .orElseGet(() -> createNewUser(registrationId, identity));

        String nameAttributeKey = switch (registrationId) {
            case "naver" -> "response";
            default -> "sub"; // google
        };

        return new AppOAuth2User(
            delegate.getAuthorities(), attributes, nameAttributeKey, appUser.getId(), identity.name(), registrationId);
    }

    private AppUser createNewUser(String registrationId, ProviderIdentity identity) {
        AppUser appUser = appUserRepository.save(new AppUser(registrationId, identity.providerUserId(), identity.name()));
        familyMemberRepository.save(new FamilyMember(appUser.getId(), identity.name(), true));
        notificationSettingRepository.save(new NotificationSetting(appUser.getId()));
        return appUser;
    }

    @SuppressWarnings("unchecked")
    private ProviderIdentity extractIdentity(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "google" -> new ProviderIdentity(
                String.valueOf(attributes.get("sub")), String.valueOf(attributes.getOrDefault("name", "구글 사용자")));
            case "naver" -> {
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                yield new ProviderIdentity(
                    String.valueOf(response.get("id")), String.valueOf(response.getOrDefault("name", "네이버 사용자")));
            }
            default -> throw new OAuth2AuthenticationException("지원하지 않는 로그인 provider: " + registrationId);
        };
    }

    private record ProviderIdentity(String providerUserId, String name) {}
}
