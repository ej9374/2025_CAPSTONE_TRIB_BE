package triB.triB.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import triB.triB.community.entity.Post;
import triB.triB.community.entity.PostType;
import triB.triB.schedule.entity.Trip;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {
    // 기본 CRUD는 JpaRepository가 제공

    /**
     * 게시글 ID와 사용자 ID로 게시글 조회
     */
    Optional<Post> findByPostIdAndUserId(Long postId, Long userId);

    /**
     * 게시글 타입별 목록 조회 (생성일 내림차순)
     */
    List<Post> findByPostTypeOrderByCreatedAtDesc(PostType postType);

    /**
     * 게시글 타입별 개수 조회
     */
    long countByPostType(PostType postType);

    /**
     * 핫 게시글 조회
     * 특정 시간 이후 작성된 게시글 중 좋아요 + 댓글 수가 가장 많은 1개 반환
     */
    @Query("SELECT p FROM Post p WHERE p.postType = :postType AND p.createdAt > :date " +
            "ORDER BY (p.likesCount + p.commentsCount) DESC, p.createdAt DESC")
    Post findTopByPostTypeAndCreatedAtAfterOrderByLikesCountDescCommentsCountDesc(
            @Param("postType") PostType postType,
            @Param("date") LocalDateTime date);

    @Query("select p.title from Post p where p.postId = :postId")
    String findTitleByPostId(@Param("postId") Long postId);

    @Query("select p from Post p left join fetch p.trip t left join fetch t.room join fetch p.user where p.userId = :userId and p.postType = :postType order by p.postId desc")
    List<Post> findByUser_UserIdAndPostTypeOrderByPostIdDesc(@Param("userId") Long userId, @Param("postType") PostType postType);
}
