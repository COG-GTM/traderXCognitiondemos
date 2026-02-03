package finos.traderx.pnlservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DocsController {

    @GetMapping("/")
    public String redirectToDocs() {
        return "redirect:/swagger-ui/index.html";
    }
}
