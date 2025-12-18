package triB.triB.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import triB.triB.community.entity.UserBlock;
import triB.triB.community.entity.UserBlockId;
import triB.triB.friendship.dto.UserResponse;

import java.util.List;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, UserBlockId> {

    /**
     * 특정 유저가 차단한 모든 유저 ID 목록 조회
     */
    @Query("SELECT ub.id.blockedUserId FROM UserBlock ub WHERE ub.id.blockerUserId = :blockerUserId")
    List<Long> findBlockedUserIdsByBlockerUserId(@Param("blockerUserId") Long blockerUserId);

    /**
     * 특정 유저가 차단한 모든 유저의 상세 정보 조회
     */
    @Query("SELECT new triB.triB.friendship.dto.UserResponse(u.userId, u.nickname, u.photoUrl) " +
           "FROM UserBlock ub JOIN ub.blockedUser u " +
           "WHERE ub.id.blockerUserId = :blockerUserId AND u.userStatus = 'ACTIVE'")
    List<UserResponse> findBlockedUsersWithDetailsByBlockerUserId(@Param("blockerUserId") Long blockerUserId);

    /**
     * 차단 관계 존재 여부 확인
     */
    boolean existsByIdBlockerUserIdAndIdBlockedUserId(Long blockerUserId, Long blockedUserId);
}
