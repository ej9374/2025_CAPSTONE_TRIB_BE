package triB.triB.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import triB.triB.community.entity.PostImage;

import java.util.List;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {
    /**
     * 게시글의 모든 이미지 조회 (표시 순서대로 정렬)
     */
    List<PostImage> findByPostIdOrderByDisplayOrderAsc(Long postId);

    /**
     * 게시글의 모든 이미지 삭제
     */
    void deleteByPostId(Long postId);


    @Query("select pi from PostImage pi where pi.postId in :postIds order by pi.postId, pi.displayOrder")
    List<PostImage> findImageUrlByPostIds(@Param("postIds") List<Long> postIds);


    @Query("select p.imageUrl from PostImage p where p.postId = :postId order by p.displayOrder asc limit 1")
    String findImageUrlByPostId(@Param("postId") Long postId);
}

