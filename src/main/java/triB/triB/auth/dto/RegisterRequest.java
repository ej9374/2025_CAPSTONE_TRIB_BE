package triB.triB.auth.dto;

import lombok.Getter;

@Getter
public class RegisterRequest {
    private String registerToken;
    private String nickname;
    private String username;
    private String photoUrl;
    private String refreshToken;
}
