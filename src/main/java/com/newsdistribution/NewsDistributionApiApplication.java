package com.newsdistribution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NewsDistributionApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(NewsDistributionApiApplication.class, args);
	}

}
