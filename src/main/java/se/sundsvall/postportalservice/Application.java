package se.sundsvall.postportalservice;

import org.springframework.boot.SpringApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import se.sundsvall.dept44.ServiceApplication;

@EnableFeignClients
@ServiceApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
