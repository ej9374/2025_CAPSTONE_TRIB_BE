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
            Object nameObj = attributes.get("name");
            if (nameObj instanceof Map) {
                Map<String, Object> nameMap = (Map<String, Object>) nameObj;
                String lastName = (String) nameMap.getOrDefault("lastName", "");
                String firstName = (String) nameMap.getOrDefault("firstName", "");
                return lastName + firstName;
            }
            return (String) nameObj;
        }
        String email = (String) attributes.get("email");
        if (email != null && email.contains("@")) {
            return email.split("@")[0];
        }
        return "apple user";
    }

    @Override
    public String getProfileImageUrl() {
        return null;
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }
}
