package com.fitflow.repo;

import com.fitflow.model.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 登录账户查询与后台账号管理的数据访问接口。 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

  Optional<UserAccount> findByUsername(String username);
  Optional<UserAccount> findByMemberId(Long memberId);
  boolean existsByUsername(String username);
  void deleteByMemberId(Long memberId);
}
