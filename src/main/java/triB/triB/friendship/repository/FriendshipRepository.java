package triB.triB.friendship.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import triB.triB.auth.entity.UserStatus;
import triB.triB.friendship.entity.Friendship;
import triB.triB.friendship.entity.FriendshipStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    boolean existsByRequester_UserIdAndAddressee_UserId(Long userId, Long addresseeId);

    @Query("select f from Friendship f where f.addressee.userId = :userId and f.friendshipStatus = :friendshipStatus and f.requester.userStatus = :userStatus order by f.createdAt asc")
    List<Friendship> findAllByAddressee_UserIdAndFriendshipStatusAndUserStatusOrderByCreatedAtAsc(@Param("userId") Long userId,@Param("friendshipStatus") FriendshipStatus friendshipStatus, @Param("userStatus") UserStatus userStatus);

    Friendship findByRequester_UserIdAndAddressee_UserIdAndFriendshipStatus(Long userId, Long addresseeId, FriendshipStatus status);
}
