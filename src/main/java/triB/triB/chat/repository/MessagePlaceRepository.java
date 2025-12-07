package triB.triB.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import triB.triB.chat.entity.MessagePlace;
import triB.triB.chat.entity.PlaceTag;

import java.util.List;

@Repository
public interface MessagePlaceRepository extends JpaRepository<MessagePlace, Long> {

    MessagePlace findByMessage_MessageId(@Param("messageId") Long messageId);

    @Query("select mp from MessagePlace mp where mp.room.roomId = :roomId order by mp.messagePlaceId desc")
    List<MessagePlace> findByRoom_RoomId(Long roomId);

    @Query("select mp from MessagePlace mp where mp.room.roomId = :roomId order by mp.messagePlaceId desc limit 5")
    List<MessagePlace> findByRoom_RoomIdLatest(Long roomId);

    // 배치조회로 메세지 리스트에서 빠르게 조회
    @Query("select mp from MessagePlace mp where mp.message.messageId in :messageIds")
    List<MessagePlace> findByMessageIds(@Param("messageIds") List<Long> messageIds);
}
