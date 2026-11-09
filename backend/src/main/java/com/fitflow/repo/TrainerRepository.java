package com.fitflow.repo;
import com.fitflow.model.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;
/** 教练资料持久层。 */
public interface TrainerRepository extends JpaRepository<Trainer,Long> {}
