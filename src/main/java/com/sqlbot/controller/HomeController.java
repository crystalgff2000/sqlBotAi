package com.sqlbot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "AI\u6570\u636e\u8d44\u4ea7\u5e73\u53f0");
        return "index";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("title", "\u6570\u636e\u4eea\u8868\u76d8");
        return "modules/dashboard";
    }
}
