package dev.ximarelli.whatsappdailygroupscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WhatsAppDailyGroupSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhatsAppDailyGroupSchedulerApplication.class, args);
    }

}
