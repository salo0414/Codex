package com.fitflow.repo;
import com.fitflow.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
/** 课程预约持久层。 */
public interface BookingRepository extends JpaRepository<Booking,Long> {}
