package codes.recursive.controller;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import java.util.Map;

@Controller("/hello")
public class HelloController {

    private String test;
    private String foo;

    public HelloController(
            @Property(name = "codes.recursive.test") String test,
            @Property(name = "codes.recursive.foo") String foo
        ) {
        this.test = test;
        this.foo = foo;
    }

    @Get(uri = "/", produces = MediaType.APPLICATION_JSON)
    public Map index() {
        return Map.of(
                "test", test,
                "foo", foo
        );
    }
}