package triB.triB.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import triB.triB.community.entity.CommentBlock;
import triB.triB.community.entity.CommentBlockId;

import java.util.List;

@Repository
public interface CommentBlockRepository extends JpaRepository<CommentBlock, CommentBlockId> {

    /**
     * 특정 유저가 차단한 모든 댓글 ID 목록 조회
     */
    @Query("SELECT cb.id.blockedCommentId FROM CommentBlock cb WHERE cb.id.blockerUserId = :blockerUserId")
    List<Long> findBlockedCommentIdsByBlockerUserId(@Param("blockerUserId") Long blockerUserId);

    /**
     * 차단 관계 존재 여부 확인
     */
    boolean existsByIdBlockerUserIdAndIdBlockedCommentId(Long blockerUserId, Long blockedCommentId);
}
