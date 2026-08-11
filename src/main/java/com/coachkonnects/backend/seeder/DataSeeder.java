package com.coachkonnects.backend.seeder;

import com.coachkonnects.backend.model.User;
import com.coachkonnects.backend.model.CoachProfile;
import com.coachkonnects.backend.model.StudentProfile;
import com.coachkonnects.backend.model.ProfileStatus;
import com.coachkonnects.backend.repository.UserRepository;
import com.coachkonnects.backend.repository.CoachProfileRepository;
import com.coachkonnects.backend.repository.StudentProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoachProfileRepository coachProfileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Override
    public void run(String... args) throws Exception {
        // Seed the hardcoded admin account if it doesn't exist
        if (userRepository.findByEmail("admin@coachkonnects.com").isEmpty()) {
            User admin = new User();
            admin.setEmail("admin@coachkonnects.com");
            admin.setPasswordHash("admin123");
            admin.setRole("ADMIN");
            userRepository.save(admin);
            System.out.println("Seeded default admin account: admin@coachkonnects.com / admin123");
        }

    }
}
