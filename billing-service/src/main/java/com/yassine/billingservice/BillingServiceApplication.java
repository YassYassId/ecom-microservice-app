package com.yassine.billingservice;

import com.yassine.billingservice.entities.Bill;
import com.yassine.billingservice.entities.ProductItem;
import com.yassine.billingservice.feign.CustomerRestClient;
import com.yassine.billingservice.feign.ProductRestClient;
import com.yassine.billingservice.model.Customer;
import com.yassine.billingservice.model.Product;
import com.yassine.billingservice.repository.BillRepository;
import com.yassine.billingservice.repository.ProductItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@SpringBootApplication
@EnableFeignClients
public class BillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(BillRepository billRepository,
                                        ProductItemRepository productItemRepository,
                                        CustomerRestClient customerRestClient,
                                        ProductRestClient productRestClient) {
        return args -> {
            Collection<Customer> customers = customerRestClient.getAllCustomers().getContent();
            Collection<Product> products = productRestClient.getAllProducts().getContent();

            customers.forEach(customer -> {
                Bill bill = Bill.builder()
                        .billingDate(new Date())
                        .customerId(customer.getId())
                        .build();
                billRepository.save(bill);

                products.forEach(product -> {
                    ProductItem productItem = ProductItem.builder()
                            .bill(bill)
                            .productId(product.getId())
                            .unitPrice(product.getPrice())
                            .quantity(1 + (int)(Math.random() * 10))
                            .build();
                    productItemRepository.save(productItem);
                });
            });
        };
    }
}
