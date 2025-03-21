package com.birmanBank.BirmanBankBackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@SpringBootApplication
public class BirmanBankBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(BirmanBankBackendApplication.class, args);
    }
}

@Controller
class WebController {
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", "Spring Boot Web Interface Works!");
        return "index";
    }
}
