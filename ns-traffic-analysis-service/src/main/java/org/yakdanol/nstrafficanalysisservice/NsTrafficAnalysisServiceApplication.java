package org.yakdanol.nstrafficanalysisservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.yakdanol.nstrafficanalysisservice")
public class NsTrafficAnalysisServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NsTrafficAnalysisServiceApplication.class, args);
	}

}
