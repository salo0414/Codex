package com.fitflow;

import com.fitflow.model.UserAccount;
import com.fitflow.repo.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
/** Spring Boot 服务入口，负责启动健身房管理系统后端。 */
public class FitflowApplication {

  public static void main(String[] args) {
    SpringApplication.run(FitflowApplication.class, args);
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** 首次启动时创建管理员账户，密码仅以 BCrypt 摘要形式入库。 */
  @Bean
  CommandLineRunner createAdmin(
    UserAccountRepository users,
    PasswordEncoder encoder,
    @Value("${fitflow.admin.username:admin}") String username,
    @Value("${fitflow.admin.password}") String password
  ) {
    return args -> {
      if (password.isBlank()) {
        throw new IllegalStateException("请通过 ADMIN_PASSWORD 设置管理员初始密码");
      }
      if (users.findByUsername(username).isEmpty()) {
        UserAccount admin = new UserAccount();
        admin.username = username;
        admin.passwordHash = encoder.encode(password);
        admin.role = "ADMIN";
        users.save(admin);
      }
    };
  }
}
