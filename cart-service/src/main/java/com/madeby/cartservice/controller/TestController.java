package com.madeby.cartservice.controller;

import com.madeby.cartservice.service.ErrorfulService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final ErrorfulService errorfulService;

    @GetMapping("/test/case1")
    public String testCase1() {
        return errorfulService.handleCase1();
    }

    @GetMapping("/test/case2")
    public String testCase2() {
        return errorfulService.handleCase2();
    }

    @GetMapping("/test/case3")
    public String testCase3() {
        return errorfulService.handleCase3();
    }
}
