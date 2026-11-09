package com.fitflow.repo;
import com.fitflow.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
/** 会员持久层，提供基础 CRUD 和仪表盘统计查询。 */
public interface MemberRepository extends JpaRepository<Member,Long> {
 long countByStatus(String status);
 @Query("select count(m) from Member m where m.createdAt >= CURRENT_DATE") long countNewToday();
}
