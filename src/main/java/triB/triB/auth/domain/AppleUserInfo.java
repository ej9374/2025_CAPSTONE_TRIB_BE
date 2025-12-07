package triB.triB.auth.domain;

import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
public class AppleUserInfo implements OAuth2UserInfo {

    private Map<String, Object> attributes;

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getProvider() {
        return "apple";
    }

    @Override
    public String getNickname() {
        if (attributes.containsKey("name")) {
            return (String) attributes.get("name");
        }
        return null;
    }

    @Override
    public String getProfileImageUrl() {
        return null;
    }
}
