package com.coachkonnects.backend;

import com.coachkonnects.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class TestJwt implements CommandLineRunner {
    @Autowired
    private JwtUtil jwtUtil;

    public static void main(String[] args) {
        SpringApplication.run(TestJwt.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("TOKEN: " + jwtUtil.generateToken("naheh25831@luhupo.com"));
        System.exit(0);
    }
}
