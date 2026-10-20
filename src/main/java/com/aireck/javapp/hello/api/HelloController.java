package com.aireck.javapp.hello.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

  @GetMapping(path = "/hello", produces = MediaType.TEXT_PLAIN_VALUE)
  public String hello() {
    return "Hello, world!";
  }
}
