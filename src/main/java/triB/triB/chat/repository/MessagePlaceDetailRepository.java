package triB.triB.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import triB.triB.chat.entity.MessagePlaceDetail;

import java.util.List;

@Repository
public interface MessagePlaceDetailRepository extends JpaRepository<MessagePlaceDetail, Long> {
    MessagePlaceDetail findByMessage_MessageId (Long messageId);

    //배치 조회
    @Query("select mpd from MessagePlaceDetail mpd where mpd.message.messageId in :messageIds")
    List<MessagePlaceDetail> findByMessageIds(@Param("messageIds") List<Long> messageIds);
}
