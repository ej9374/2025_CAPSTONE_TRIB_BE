package triB.triB.auth.domain;

import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
public class KakaoUserInfo implements OAuth2UserInfo {

    private Map<String, Object> attributes;

    @Override
    public String getProviderId() {
        Object id = attributes.get("id");          // Long/Integer/String 가능
        return String.valueOf(id);
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getNickname() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        Object nickname = profile.get("nickname");
        return (String) nickname;
    }

    @Override
    public String getProfileImageUrl() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        Object nickname = profile.get("profile_image_url");
        return (String) nickname;
    }

    @Override
    public String getEmail() {return (String) attributes.get("email");}

}
