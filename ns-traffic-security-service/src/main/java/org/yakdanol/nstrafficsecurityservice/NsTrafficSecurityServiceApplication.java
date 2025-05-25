package org.yakdanol.nstrafficsecurityservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.yakdanol.nstrafficsecurityservice")
public class NsTrafficSecurityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NsTrafficSecurityServiceApplication.class, args);
    }

}
