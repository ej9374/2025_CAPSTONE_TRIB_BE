package triB.triB.auth.repository;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import triB.triB.auth.entity.IsAlarm;
import triB.triB.auth.entity.Token;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {

    Optional<Token> findByUser_UserId(Long userId);

    @Query("select t from Token t where t.user.userId = :userId and t.user.isAlarm = :isAlarm")
    Token findByUser_UserIdAndUser_IsAlarm(@Param("userId") Long userId, @Param("isAlarm") IsAlarm isAlarm);

    List<Token> findAllByUser_UserIdInAndUser_IsAlarm(List<Long> userIds, IsAlarm isAlarm);
}