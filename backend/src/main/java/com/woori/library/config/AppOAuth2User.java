package com.woori.library.config;

import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

/** DefaultOAuth2User에 내부 app_user.id를 얹어, 컨트롤러에서 provider별 속성 파싱 없이 바로 쓸 수 있게 한다. */
public class AppOAuth2User extends DefaultOAuth2User {

    private final Long appUserId;
    private final String displayName;
    private final String provider;

    public AppOAuth2User(
        Collection<? extends GrantedAuthority> authorities,
        Map<String, Object> attributes,
        String nameAttributeKey,
        Long appUserId,
        String displayName,
        String provider) {
        super(authorities, attributes, nameAttributeKey);
        this.appUserId = appUserId;
        this.displayName = displayName;
        this.provider = provider;
    }

    public Long getAppUserId() {
        return appUserId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getProvider() {
        return provider;
    }
}
