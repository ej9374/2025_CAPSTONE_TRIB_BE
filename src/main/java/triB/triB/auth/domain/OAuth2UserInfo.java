package triB.triB.auth.domain;

public interface OAuth2UserInfo {
    String getProviderId();
    String getProvider();
    String getNickname();
    String getProfileImageUrl();
    String getEmail();
}
