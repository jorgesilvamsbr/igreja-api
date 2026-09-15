package com.igreja.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PingController {

  @GetMapping("/ping")
  @ResponseStatus(HttpStatus.OK)
  public String ping() {
    return "pong";
  }
}
