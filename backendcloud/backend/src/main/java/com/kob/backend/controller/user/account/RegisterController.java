package com.kob.backend.controller.user.account;

import com.kob.backend.pojo.User;
import com.kob.backend.service.user.account.RegisterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RegisterController {
    @Autowired
    private RegisterService registerService;

    @PostMapping("/user/account/register/")
    public Map<String,String> register(@RequestParam Map<String,String> map){
        System.out.println(map);
        String username = map.get("username");
        String password = map.get("password");
        String confirmPassword = map.get("confirmedPassword");
        System.out.println("username: "+username);
        System.out.println("password: "+password);
        System.out.println("confirmPassword: "+confirmPassword);
        return registerService.register(username,password,confirmPassword);
    }
}
