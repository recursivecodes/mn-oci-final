package codes.recursive.controller;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.annotation.*;

@Controller("/hello")
public class HelloController {

    private String test;

    public HelloController(@Property(name = "codes.recursive.test") String test) {
        this.test = test;
    }

    @Get(uri="/", produces="text/plain")
    public String index() {
        return test;
    }
}