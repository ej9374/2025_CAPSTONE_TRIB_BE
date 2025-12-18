package triB.triB.community.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import triB.triB.auth.repository.UserRepository;
import triB.triB.community.entity.UserBlock;
import triB.triB.community.entity.UserBlockId;
import triB.triB.community.repository.UserBlockRepository;
import triB.triB.friendship.dto.UserResponse;
import triB.triB.global.exception.CustomException;
import triB.triB.global.exception.ErrorCode;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserBlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;

    /**
     * 유저 차단
     */
    @Transactional
    public void blockUser(Long blockerUserId, Long blockedUserId) {
        // 자기 자신 차단 방지
        if (blockerUserId.equals(blockedUserId)) {
            throw new CustomException(ErrorCode.CANNOT_BLOCK_YOURSELF);
        }

        // 차단할 유저 존재 확인
        userRepository.findById(blockedUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 이미 차단된 경우 무시 (중복 차단 방지)
        if (userBlockRepository.existsByIdBlockerUserIdAndIdBlockedUserId(blockerUserId, blockedUserId)) {
            return;
        }

        UserBlockId id = new UserBlockId(blockerUserId, blockedUserId);
        UserBlock userBlock = UserBlock.builder()
                .id(id)
                .build();
        userBlockRepository.save(userBlock);
    }

    /**
     * 유저 차단 해제
     */
    @Transactional
    public void unblockUser(Long blockerUserId, Long blockedUserId) {
        UserBlockId id = new UserBlockId(blockerUserId, blockedUserId);
        userBlockRepository.deleteById(id);
    }

    /**
     * 특정 유저가 차단한 모든 유저 ID 목록 조회
     */
    public List<Long> getBlockedUserIds(Long blockerUserId) {
        return userBlockRepository.findBlockedUserIdsByBlockerUserId(blockerUserId);
    }

    /**
     * 특정 유저가 차단한 모든 유저의 상세 정보 조회
     */
    public List<UserResponse> getBlockedUsers(Long blockerUserId) {
        return userBlockRepository.findBlockedUsersWithDetailsByBlockerUserId(blockerUserId);
    }

    /**
     * 차단 여부 확인
     */
    public boolean isBlocked(Long blockerUserId, Long blockedUserId) {
        return userBlockRepository.existsByIdBlockerUserIdAndIdBlockedUserId(blockerUserId, blockedUserId);
    }
}
