package triB.triB.user.dto;

import lombok.Getter;

@Getter
public class UpdateProfileRequest {
    private String nickname;
    private Boolean isDeleted;
}
