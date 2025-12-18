package triB.triB.auth.domain;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@AllArgsConstructor
public class GoogleUserInfo implements OAuth2UserInfo{

    private Map<String, Object> attributes;

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getProvider() {
        return "google";
    }

    @Override
    public String getNickname() {
        return (String) attributes.get("name");
    }

    @Override
    public String getProfileImageUrl() {
        return (String) attributes.get("picture");
    }

    @Override
    public String getEmail() {return (String) attributes.get("email");}
}
