package com.yassine.inventoryservice;

import com.yassine.inventoryservice.entities.Product;
import com.yassine.inventoryservice.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.UUID;

@SpringBootApplication
public class InventoryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryServiceApplication.class, args);
	}

	@Bean
	CommandLineRunner commandLineRunner(ProductRepository repository) {
		return args -> {
			repository.save(Product.builder()
					.id(UUID.randomUUID().toString())
					.name("Computer")
					.price(6500)
					.quantity(12)
					.build());
			repository.save(Product.builder()
					.id(UUID.randomUUID().toString())
					.name("Printer")
					.price(1200)
					.quantity(15)
					.build());
			repository.save(Product.builder()
					.id(UUID.randomUUID().toString())
					.name("Smartphone")
					.price(3200)
					.quantity(20)
					.build());

			repository.findAll().forEach(product -> {
				System.out.println("--------------------------");
				System.out.println(product.toString());
				System.out.println("--------------------------");
			});
		};
	}
}
