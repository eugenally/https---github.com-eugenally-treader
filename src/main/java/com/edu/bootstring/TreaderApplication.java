package com.edu.bootstring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // M7: 배치 작업 활성화
public class TreaderApplication {

	public static void main(String[] args) {
		SpringApplication.run(TreaderApplication.class, args);
	}

}
