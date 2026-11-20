package com.fitflow.repo;

import com.fitflow.model.FitnessClass;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 课程持久层，支持近期课程和指定日期排课数量查询。 */
public interface FitnessClassRepository
  extends JpaRepository<FitnessClass, Long>
{
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from FitnessClass c where c.id = :id")
  Optional<FitnessClass> findByIdForUpdate(@Param("id") Long id);
  List<FitnessClass> findTop6ByStartTimeAfterOrderByStartTimeAsc(
    LocalDateTime time
  );
  long countByStartTimeBetween(LocalDateTime from, LocalDateTime to);
}
