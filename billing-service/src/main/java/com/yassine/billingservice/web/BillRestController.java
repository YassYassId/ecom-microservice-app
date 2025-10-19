package com.yassine.billingservice.web;


import com.yassine.billingservice.entities.Bill;
import com.yassine.billingservice.feign.CustomerRestClient;
import com.yassine.billingservice.feign.ProductRestClient;
import com.yassine.billingservice.repository.BillRepository;
import com.yassine.billingservice.repository.ProductItemRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class BillRestController {
    BillRepository billRepository;
    ProductItemRepository productItemRepository;
    CustomerRestClient customerRestClient;
    ProductRestClient productRestClient;

    @GetMapping("/bills/{id}")
    public Bill getBill(@PathVariable Long id){
        Bill bill = billRepository.findById(id).get();
        bill.setCustomer(customerRestClient.getCustomerById(bill.getCustomerId()));
        bill.getProductItems().forEach(pi->{
            pi.setProducts(productRestClient.getProductById(pi.getProductId().toString()));
        });
        return bill;
    }
}
