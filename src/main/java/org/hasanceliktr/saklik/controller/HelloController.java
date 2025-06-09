package org.hasanceliktr.saklik.controller; // Doğru paket

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api") // Controller seviyesinde /api path'i
public class HelloController {

    @GetMapping("/hello") // Metod seviyesinde /hello path'i
    public String sayHello() {
        return "Merhaba Saklık Backend!";
    }
}