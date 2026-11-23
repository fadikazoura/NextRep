package com.fadikazoura.nextrep.demo;

import com.fadikazoura.nextrep.auth.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DemoController {

    private final DemoAccountService demoAccountService;

    /** Creates a private demo account full of sample data and returns a token for it (no sign-up needed). */
    @PostMapping("/api/auth/demo")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse createDemoAccount() {
        return demoAccountService.createDemoAccount();
    }
}
