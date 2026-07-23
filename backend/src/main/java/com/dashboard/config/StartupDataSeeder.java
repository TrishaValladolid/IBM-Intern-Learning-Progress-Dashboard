package com.dashboard.config;

import com.dashboard.entity.User;
import com.dashboard.repository.UserRepository;
import com.dashboard.security.PasswordUtil;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;

import java.util.logging.Logger;

/**
 * Seeds default accounts on first boot so the system is usable immediately.
 * Runs only when the user table is empty. Passwords are stored hashed.
 *
 * Default credentials (change after first login in a real deployment):
 *   admin   / admin123    (ADMIN)
 *   trainer / trainer123  (TRAINER)
 */
@Singleton
@Startup
public class StartupDataSeeder {

    private static final Logger LOG = Logger.getLogger(StartupDataSeeder.class.getName());

    @Inject
    private UserRepository userRepository;

    @PostConstruct
    public void seed() {
        if (userRepository.count() > 0) {
            return;
        }

        userRepository.save(new User(
                "admin",
                PasswordUtil.hash("admin123"),
                User.Role.ADMIN,
                "Program Coordinator"));

        userRepository.save(new User(
                "trainer",
                PasswordUtil.hash("trainer123"),
                User.Role.TRAINER,
                "Default Trainer"));

        LOG.info("Seeded default users: admin/admin123 (ADMIN), trainer/trainer123 (TRAINER)");
    }
}
