package codes.recursive.controller;

import codes.recursive.domain.Person;
import codes.recursive.repository.PersonRepository;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import java.util.Map;

@Controller("/hello")
public class HelloController {

    private String test;
    private String foo;
    private final PersonRepository personRepository;

    public HelloController(
            @Property(name = "codes.recursive.test") String test,
            @Property(name = "codes.recursive.foo") String foo,
            PersonRepository personRepository
        ) {
        this.test = test;
        this.foo = foo;
        this.personRepository = personRepository;
    }

    @Get(uri = "/", produces = MediaType.APPLICATION_JSON)
    public Map index() {
        return Map.of(
                "test", test,
                "foo", foo
        );
    }

    @Get("/persons")
    public HttpResponse getPersons() {
        return HttpResponse.ok(
                personRepository.findAll()
        );
    }

    @Get("/person/{id}")
    public HttpResponse getPerson(Long id) {
        return HttpResponse.ok(
                personRepository.findById(id)
        );
    }

    @Post("/person")
    public HttpResponse savePerson(Person person) {
        return HttpResponse.created(
                personRepository.save(person)
        );
    }
}