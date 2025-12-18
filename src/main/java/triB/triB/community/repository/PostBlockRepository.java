package triB.triB.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import triB.triB.community.entity.PostBlock;
import triB.triB.community.entity.PostBlockId;

import java.util.List;

@Repository
public interface PostBlockRepository extends JpaRepository<PostBlock, PostBlockId> {

    /**
     * 특정 유저가 차단한 모든 게시글 ID 목록 조회
     */
    @Query("SELECT pb.id.blockedPostId FROM PostBlock pb WHERE pb.id.blockerUserId = :blockerUserId")
    List<Long> findBlockedPostIdsByBlockerUserId(@Param("blockerUserId") Long blockerUserId);

    /**
     * 차단 관계 존재 여부 확인
     */
    boolean existsByIdBlockerUserIdAndIdBlockedPostId(Long blockerUserId, Long blockedPostId);
}
