package triB.triB.friendship.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import triB.triB.auth.entity.User;
import triB.triB.auth.entity.UserStatus;
import triB.triB.friendship.entity.Friend;

import java.util.List;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {

    @Query("select f.friend from Friend f where f.user.userId = :userId and f.friend.userStatus = :userStatus order by f.friend.nickname asc")
    List<User> findAllFriendByUserAndUserStatus(@Param("userId") Long userId, @Param("userStatus") UserStatus userStatus);

    @Query("select f.friend from Friend f where f.user.userId = :userId " +
            "and f.friend.userStatus = :userStatus and" +
            " lower(f.friend.nickname) like lower(concat('%', :nickname, '%')) order by f.friend.nickname asc")
    List<User> findAllFriendByUserAndFriend_NicknameAndUserStatus(@Param("userId") Long userId, @Param("nickname") String nickname, @Param("userStatus") UserStatus userStatus);

    boolean existsByUser_UserIdAndFriend_UserId(Long userId, Long friendId);
}
