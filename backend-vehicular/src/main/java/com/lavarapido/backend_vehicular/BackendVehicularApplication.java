package com.lavarapido.backend_vehicular;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.lavarapido.backend_vehicular.pagos.config.WompiProperties;

@SpringBootApplication
@EnableConfigurationProperties(WompiProperties.class)
public class BackendVehicularApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendVehicularApplication.class, args);
	}

}
