package com.bank.account.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Convenience: /swagger-ui.html opens the UI pointed at our static contract. */
@Controller
class SwaggerUiRedirect {

    @GetMapping("/swagger-ui.html")
    String swaggerUi() {
        return "redirect:/webjars/swagger-ui/index.html?url=/account-api.yaml";
    }
}
