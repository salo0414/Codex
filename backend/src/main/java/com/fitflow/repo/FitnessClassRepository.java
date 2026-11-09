package com.fitflow.repo;
import com.fitflow.model.FitnessClass;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
/** 课程持久层，支持近期课程和指定日期排课数量查询。 */
public interface FitnessClassRepository extends JpaRepository<FitnessClass,Long> {
 List<FitnessClass> findTop6ByStartTimeAfterOrderByStartTimeAsc(LocalDateTime time);
 long countByStartTimeBetween(LocalDateTime from, LocalDateTime to);
}
