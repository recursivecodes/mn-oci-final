package codes.recursive.domain;

import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.DateUpdated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "mn-oci-demo-persons-1")
@NoArgsConstructor
public class Person {
    @Id
    @GeneratedValue
    @Getter
    @Setter
    Long id;
    @Getter
    @Setter
    @Size(min = 1, max = 10)
    String firstName;
    @Getter
    @Setter
    @Size(min = 1, max = 10)
    String lastName;
    @Getter
    @Setter
    @Min(1L)
    @Max(125L)
    int age;
    @Getter
    @Setter
    @DateCreated
    LocalDateTime dateCreated;
    @Getter
    @Setter
    @DateUpdated
    LocalDateTime lastUpdated;

    public Person(@Size(min = 1, max = 10) String firstName, @Size(min = 1, max = 10) String lastName, @Min(1L) @Max(125L) int age) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
    }

}
