package triB.triB.room.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import triB.triB.auth.entity.IsAlarm;
import triB.triB.auth.entity.User;
import triB.triB.auth.entity.UserStatus;
import triB.triB.room.entity.Room;
import triB.triB.room.entity.RoomStatus;
import triB.triB.room.entity.UserRoom;
import triB.triB.room.entity.UserRoomId;

import java.util.List;

@Repository
public interface UserRoomRepository extends JpaRepository<UserRoom, UserRoomId> {

    @Query("select ur from UserRoom ur join fetch ur.user u join fetch ur.room r where u.userId = :userId " +
            "order by coalesce((select max(m.createdAt) from Message m where m.room = ur.room), r.createdAt) desc")
    List<UserRoom> findAllWithRoomAndUsersByUser_UserId(@Param("userId") Long userId);

    @Query("select ur from UserRoom ur join fetch ur.user u join fetch ur.room r where u.userId = :userId and lower(r.roomName) like lower(concat('%', :roomName, '%')) " +
            "order by coalesce((select max(m.createdAt) from Message m where m.room = ur.room), r.createdAt) desc")
    List<UserRoom> findAllWithRoomAndUsersByUser_UserIdAndRoom_RoomName(@Param("userId") Long userId, @Param("roomName") String roomName);

    @Query("select ur from UserRoom ur where ur.room.roomId in :roomIds and ur.user.userStatus = :userStatus order by ur.user.nickname asc")
    List<UserRoom> findAllWithUsersByRoomIdsAndUserStatus(@Param("roomIds") List<Long> roomIds, @Param("userStatus") UserStatus userStatus);

    @Query("select ur.user from UserRoom ur where ur.room.roomId = :roomId order by ur.user.nickname asc")
    List<User> findUsersByRoomId(@Param("roomId") Long roomId);

    @Query("select ur.user from UserRoom ur where ur.room.roomId = :roomId and ur.user.isAlarm = :isAlarm")
    List<User> findUsersByRoomIdAndIsAlarm(@Param("roomId") Long roomId, @Param("isAlarm") IsAlarm isAlarm);

    boolean existsByUser_UserIdAndRoom_RoomId(Long userId, Long roomId);

    UserRoom findByUser_UserIdAndRoom_RoomId(Long userId, Long roomId);

    @Query("select count(ur.user) from UserRoom ur where ur.room.roomId = :roomId and ur.user.userStatus = :userStatus")
    Integer countByRoom_RoomIdAndUserStatus(@Param("roomId") Long roomId, @Param("userStatus") UserStatus userStatus);

    @Query("select ur from UserRoom ur where ur.room.roomId = :roomId and ur.user.userId != :userId and ur.user.userStatus = :userStatus order by ur.user.nickname asc")
    List<UserRoom> findByRoom_RoomIdAndNotUser_UserIdAndUserStatus(Long roomId, Long userId, UserStatus userStatus);

    @Query(value = "select * from user_room ur where user_id = :userId and room_id = :roomId", nativeQuery = true)
    UserRoom findByUserIdAndRoomIdWithoutFilter(@Param("userId") Long userId, @Param("roomId") Long roomId);

    @Query("select u.user.photoUrl from UserRoom u where u.room.roomId = :roomId and u.user.userStatus = :userStatus order by u.user.nickname asc")
    List<String> findAllByRoom_RoomIdAndUserStatus(@Param("roomId") Long roomId, @Param("userStatus") UserStatus userStatus);
}
