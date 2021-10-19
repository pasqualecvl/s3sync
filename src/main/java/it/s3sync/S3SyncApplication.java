package it.s3sync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class S3SyncApplication {

	public static void main(String[] args) {
		SpringApplication.run(S3SyncApplication.class, args);
	}

}
