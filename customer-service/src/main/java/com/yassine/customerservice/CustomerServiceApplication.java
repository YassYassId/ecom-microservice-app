package com.yassine.customerservice;

import com.yassine.customerservice.config.CustomerConfigParams;
import com.yassine.customerservice.entities.Customer;
import com.yassine.customerservice.repository.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(CustomerConfigParams.class)
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(CustomerRepository repository) {
        return args -> {
            repository.save(Customer.builder()
                    .name("Yassine")
                    .email("yassine@test.com")
                    .build());
            repository.save(Customer.builder()
                    .name("Ahmed")
                    .email("ahmed@gmail.com")
                    .build());
            repository.save(Customer.builder()
                    .name("mohammed")
                    .email("mohammed@gmail.com")
                    .build());

            repository.findAll().forEach(customer -> {
                        System.out.println("--------------------------");
                        System.out.println(customer.getId());
                        System.out.println(customer.getName());
                        System.out.println(customer.getEmail());
                        System.out.println("--------------------------");
            });
        };
    }
}
