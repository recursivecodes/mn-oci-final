# mn-oci-final

![mn-oci-final](https://github.com/recursivecodes/mn-oci-final/workflows/mn-oci-final/badge.svg)

## Step 1 - Create App

See branch step 1: https://github.com/recursivecodes/mn-oci-final/tree/step-1

### 1.1

Create an app with the CLI.

```shell script
mn create-app codes.recursive.mn-oci --features data-jpa --jdk 11
```

### 1.2

Add a controller.

```shell script
mn create-controller codes.recursive.controller.Hello
```

### 1.3 

Add run/debug config:

[run-debug](https://objectstorage.us-phoenix-1.oraclecloud.com/n/toddrsharp/b/readme-assets/o/run-debug.png)

### 1.4

Add a config var to `resources/application.yml`:

```yaml
codes:
  recursive:
    test: 'This is localhost'
```

### 1.5

Add a constructor to the controller and inject the config var.

```java
private String test;

public HelloController(@Property(name = "codes.recursive.test") String test) {
    this.test = test;
}
```

### 1.6

Modify "/" endpoint to return config var.

```java 
@Get(uri="/", produces="text/plain")
public String index() {
    return test;
}
```

### 1.7

Run & test...

```shell script 
$ curl -i http://localhost:8080/hello                                                                                                                                           
HTTP/1.1 200 OK
Date: Thu, 14 May 2020 16:58:05 GMT
content-type: text/plain
content-length: 17
connection: keep-alive

This is localhost 
```

## Step 2 - Setup Build & Deploy

See branch step 2: https://github.com/recursivecodes/mn-oci-final/tree/step-2

### Prerequisites: 

The following GitHub secrets need to be created ahead of time:

| Name                      |
| :------------------------ |
| OCI_USER_OCID             |
| OCI_FINGERPRINT           |
| OCI_PASSPHRASE            |
| OCI_REGION                |
| OCI_TENANCY_OCID          |
| OCI_KEY_FILE              |
| VM_SSH_PUB_KEY            |
| VM_SSH_PRIVATE_KEY        |
| VM_COMPARTMENT_OCID       |
| VM_AVAILABILITY_DOMAIN    |
| VM_SHAPE                  |
| VM_CUSTOM_IMAGE_OCID      |
| VM_SUBNET_OCID            |
| WALLET_CWALLET            |
| WALLET_EWALLET            |
| WALLET_KEYSTORE           |
| WALLET_OJDBC              |
| WALLET_SQLNET             |
| WALLET_TNSNAMES           |
| WALLET_TRUSTSTORE         |

### 2.1

Add `.github/workflows/oracle-cloud.yaml`

### 2.2

Populate `oracle-cloud.yaml`.

#### 2.2.1
```yaml
name: mn-oci-final
on:
  push:
    branches:
      - '*'
jobs:
  build-job:
    name: Build Job
    runs-on: ubuntu-latest
```
#### 2.2.2
```yaml
    steps:

      - name: 'Checkout'
        uses: actions/checkout@v2

      - name: 'Setup Java 11'
        uses: actions/setup-java@v1
        with:
          java-version: 11

      - name: 'Assemble JAR'
        run: |
          ./gradlew assemble
```
#### 2.2.3
```yaml
      - name: 'Get Version Number'
        run: |
          echo "::set-env name=VERSION::$(./gradlew properties -q | grep "version:" | awk '{print $2}')"

      - name: 'Publish JAR'
        uses: actions/upload-artifact@v2-preview
        with:
          name: 'mn-oci-${{env.VERSION}}-all.jar'
          path: build/libs/*-all.jar
```

#### 2.2.4
```yaml
      - name: 'Write Config & Key Files'
        run: |
          mkdir ~/.oci
          echo "[DEFAULT]" >> ~/.oci/config
          echo "user=${{secrets.OCI_USER_OCID}}" >> ~/.oci/config
          echo "fingerprint=${{secrets.OCI_FINGERPRINT}}" >> ~/.oci/config
          echo "pass_phrase=${{secrets.OCI_PASSPHRASE}}" >> ~/.oci/config
          echo "region=${{secrets.OCI_REGION}}" >> ~/.oci/config
          echo "tenancy=${{secrets.OCI_TENANCY_OCID}}" >> ~/.oci/config
          echo "key_file=~/.oci/key.pem" >> ~/.oci/config
          echo "${{secrets.OCI_KEY_FILE}}" >> ~/.oci/key.pem
          echo "${{secrets.VM_SSH_PUB_KEY}}" >> /home/runner/.oci/id_vm.pub

      - name: 'Install OCI CLI'
        run: |
          curl -L -O https://raw.githubusercontent.com/oracle/oci-cli/master/scripts/install/install.sh
          chmod +x install.sh
          ./install.sh --accept-all-defaults
          echo "::add-path::/home/runner/bin"
          exec -l $SHELL

      - name: 'Fix Config File Permissions'
        run: |
          oci setup repair-file-permissions --file /home/runner/.oci/config
          oci setup repair-file-permissions --file /home/runner/.oci/key.pem
```

#### 2.2.5

```yaml
      - name: 'Check Existing Instance'
        run: |
          echo "::set-env name=INSTANCE_OCID::$( \
            oci compute instance list \
            --lifecycle-state RUNNING \
            --compartment-id ${{secrets.VM_COMPARTMENT_OCID}} \
            --display-name mn-oci \
            --query "data [0].id" \
            --raw-output \
          )"

      - name: 'Create Instance'
        if: ${{!env.INSTANCE_OCID}}
        run: |
          echo "::set-env name=INSTANCE_OCID::$( \
            oci compute instance launch \
              --compartment-id ${{secrets.VM_COMPARTMENT_OCID}} \
              --availability-domain ${{secrets.VM_AVAILABILITY_DOMAIN}} \
              --shape ${{secrets.VM_SHAPE}} \
              --assign-public-ip true \
              --display-name mn-oci \
              --image-id ${{secrets.VM_CUSTOM_IMAGE_OCID}} \
              --ssh-authorized-keys-file /home/runner/.oci/id_vm.pub \
              --subnet-id ${{secrets.VM_SUBNET_OCID}} \
              --wait-for-state RUNNING \
              --query "data.id" \
              --raw-output \
          )"
```

#### 2.2.6

```yaml
      - name: 'Get Instance IP'
        run: |
          echo "::set-env name=INSTANCE_IP::$( \
            oci compute instance list-vnics \
            --instance-id ${{env.INSTANCE_OCID}} \
            --query 'data [0]."public-ip"' \
            --raw-output \
          )"
          echo "1"
```

#### 2.2.7
```yaml
      - name: 'Wait for SSH'
        run: |
          while ! nc -w5 -z ${{ env.INSTANCE_IP }} 22; do
                  sleep 5
                  echo "SSH not available..."
          done; echo "SSH ready!"
```

#### 2.2.8

```yaml
      - name: 'Stop App'
        uses: appleboy/ssh-action@master
        with:
          host: ${{ env.INSTANCE_IP }}
          username: opc
          key: ${{ secrets.VM_SSH_PRIVATE_KEY }}
          timeout: 300s
          script: |
            pid=`ps aux | grep "[m]n-oci.jar" | awk '{print $2}'`
            if [ "$pid" == "" ]; then
              echo "Process not found"
            else
              kill -9 $pid
            fi
            sudo mkdir -p /app
```

#### 2.2.9

```yaml
      - name: 'Push JAR'
        uses: appleboy/scp-action@master
        with:
          host: ${{ env.INSTANCE_IP }}
          username: opc
          key: ${{ secrets.VM_SSH_PRIVATE_KEY }}
          source: "build/libs/mn-oci-${{env.VERSION}}-all.jar"
          target: "app"
          strip_components: 2
```

#### 2.2.10

```yaml
      - name: 'Start App'
        uses: appleboy/ssh-action@master
        with:
          host: ${{ env.INSTANCE_IP }}
          username: opc
          key: ${{ secrets.VM_SSH_PRIVATE_KEY }}
          script: |
            sudo mv ~/app/mn-oci-${{env.VERSION}}-all.jar /app/mn-oci.jar
            nohup java -jar /app/mn-oci.jar > output.$(date --iso).log 2>&1 &
```

### 2.3 

Create `resources/application-oraclecloud.yaml` and populate.

```yaml
codes:
  recursive:
    test: 'This is oracle cloud'
```

### 2.4 

Push to GitHub and observe build. When complete, hit the `/hello` endpoint in the cloud and observe the value returned is from `application-oracle-cloud.yaml` instead of the value from `application.yaml`.

## Step 3 - OCI Vault as Distributed Config

See branch step 3: https://github.com/recursivecodes/mn-oci-final/tree/step-3

### 3.1

Add dependencies:

```groovy
implementation "io.micronaut:micronaut-discovery-client"
implementation group: 'com.oracle.oci.sdk', name: 'oci-java-sdk-vault', version: '1.17.3'
implementation group: 'com.oracle.oci.sdk', name: 'oci-java-sdk-secrets', version: '1.17.3'
implementation group: 'com.oracle.oci.sdk', name: 'oci-java-sdk-common', version: '1.17.3'
```

### 3.2

Create vault & secret(s).

#### 3.2.1

Click 'Security' -> 'Vault' from the OCI console.

![vault-1](https://objectstorage.us-phoenix-1.oraclecloud.com/n/toddrsharp/b/readme-assets/o/vault-1.png)

#### 3.2.2

Click 'Create Vault'.

![vault-2](https://objectstorage.us-phoenix-1.oraclecloud.com/n/toddrsharp/b/readme-assets/o/vault-2.png)

#### 3.2.3

Name the vault and click 'Create'.

![vault-3](https://objectstorage.us-phoenix-1.oraclecloud.com/n/toddrsharp/b/readme-assets/o/vault-3.png)

#### 3.2.4

Select the newly created vault.

![vault-4](https://objectstorage.us-phoenix-1.oraclecloud.com/n/toddrsharp/b/readme-assets/o/vault-4.png)

#### 3.2.5

Click 'Create Key'.

![vault-5](https://objectstorage.us-phoenix-1.oraclecloud.com/n/toddrsharp/b/readme-assets/o/vault-5.png)

#### 3.2.6

Name the key and select algorithm and shape.

![vault-6](https://objectstorage.us-phoenix-1.oraclecloud.com/n/toddrsharp/b/readme-assets/o/vault-6.png)

#### 3.2.7

Click 'Create Secret'.

![vault-7](https://objectstorage.us-phoenix-1.oraclecloud.com/n/toddrsharp/b/readme-assets/o/vault-7.png)

#### 3.2.8

Give the secret a name (1), choose the encryption key (2), enter the secret contents in plain text (3) and click 'Create Secret' (4).

![vault-8](https://objectstorage.us-phoenix-1.oraclecloud.com/n/toddrsharp/b/readme-assets/o/vault-12.png)

#### 3.2.9

Copy the vault OCID for use in step 3.3.

![vault-9](https://objectstorage.us-phoenix-1.oraclecloud.com/n/toddrsharp/b/readme-assets/o/vault-9.png)

#### 3.2.10

Click 'Identity' -> 'Compartments' in the OCI console sidebar.

![vault-10](https://objectstorage.us-phoenix-1.oraclecloud.com/n/toddrsharp/b/readme-assets/o/vault-10.png)

#### 3.2.11

Find the compartment that the vault resides in and copy the compartment OCID for use in step 3.3.

![vault-11](https://objectstorage.us-phoenix-1.oraclecloud.com/n/toddrsharp/b/readme-assets/o/vault-11.png)

### 3.3

Create config at `src/resources/bootstrap.yml`.

```yaml
micronaut:
  application:
    name: vault-test
  config-client:
    enabled: true
oraclecloud:
  vault:
    config:
      enabled: true
    vaults:
      - ocid: ocid1.vault.oc1.phx.a5pmffwuaafna.abyhqljsshgiqtcpsmzdv7do6s2lcm55qdym7avmj5ipmdk7scuas5qsfk5q
        compartment-ocid: ocid1.compartment.oc1..aaaaaaaa7lzppsdxt6j56zhpvy6u5gyrenwyc2e2h4fak5ydvv6kt7anizbq
    use-instance-principal: false
    path-to-config: ~/.oci/config
    profile: DEFAULT
    region: US-PHOENIX-1
```

### 3.4

Modify `application.yml` to use vault secret as configuration variable.

```yaml
codes:
  recursive:
    test: 'This is localhost'
    foo: ${FOO}
```

### 3.5

Modify controller to return distributed config var.

```java
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
```

### 3.6

Run application, hit endpoint, confirm.

```json
{
  "foo": "BAR",
  "test": "This is localhost"
}
```

### 3.7

Add step to `.github/workflows/oracle-cloud.yaml` to create OCI config directory on VM.

```yaml
- name: 'Create OCI Config On VM'
  uses: appleboy/ssh-action@master
  with:
    host: ${{ env.INSTANCE_IP }}
    username: opc
    key: ${{ secrets.VM_SSH_PRIVATE_KEY }}
    timeout: 30s
    script: |
      mkdir -p ~/.oci
      echo "[DEFAULT]" >> ~/.oci/config-tmp
      echo "user=${{secrets.OCI_USER_OCID}}" >> ~/.oci/config-tmp
      echo "fingerprint=${{secrets.OCI_FINGERPRINT}}" >> ~/.oci/config-tmp
      echo "pass_phrase=${{secrets.OCI_PASSPHRASE}}" >> ~/.oci/config-tmp
      echo "region=${{secrets.OCI_REGION}}" >> ~/.oci/config-tmp
      echo "tenancy=${{secrets.OCI_TENANCY_OCID}}" >> ~/.oci/config-tmp
      echo "key_file=~/.oci/key.pem" >> ~/.oci/config-tmp
      mv ~/.oci/config-tmp ~/.oci/config
      echo "${{secrets.OCI_KEY_FILE}}" >> ~/.oci/key-tmp.pem
      mv ~/.oci/key-tmp.pem ~/.oci/key.pem
```

## Step 4 - Configure App & Dependencies

See branch step 4: https://github.com/recursivecodes/mn-oci-final/tree/step-4

### 4.1

Change connection pool dependency:

From:

```groovy
implementation("io.micronaut.configuration:micronaut-jdbc-tomcat")
```

To: 

```groovy
implementation("io.micronaut.configuration:micronaut-jdbc-ucp")
```

#### 4.1.1

Add ojdbc dependency:

```groovy
implementation("com.oracle.database.jdbc:ojdbc10:19.6.0.0")
```

### 4.2

Add system properties to all `JavaExec` tasks in `build.gradle`:

```groovy 
tasks.withType(JavaExec) {
    classpath += configurations.developmentOnly
    jvmArgs('-XX:TieredStopAtLevel=1', '-Dcom.sun.management.jmxremote')
    systemProperties System.properties
}
```

### 4.3

Modify datasource.

#### 4.3.1 

Create vault secret containing the DB password.

![vault-8](https://objectstorage.us-phoenix-1.oraclecloud.com/n/toddrsharp/b/readme-assets/o/vault-8.png)

#### 4.3.2

Modify datasource in `application.yml`:

From:

```yaml
datasources:
  default:
    url: jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE
    driverClassName: org.h2.Driver
    username: sa
    password: ''
    schema-generate: CREATE_DROP
    dialect: H2
```

To: 

```yaml
datasources:
  default:
    url: jdbc:oracle:thin:@barnevents_low?TNS_ADMIN=/wallet
    connectionFactoryClassName: oracle.jdbc.pool.OracleDataSource
    username: testuser
    password: ${DB_PASSWORD}
    schema-generate: CREATE_DROP
    dialect: ORACLE
    minPoolSize: 1
    maxPoolSize: 10
```

### 4.4

Disable FAN in `Application.java`:

```java
System.setProperty("oracle.jdbc.fanEnabled", "false");
```

## Step 5 - Add Model, Repo & CRUD Methods To Controller

See branch step 5: https://github.com/recursivecodes/mn-oci-final/tree/step-5

### 5.1

Add lombok dependencies to `build.gradle`.

```groovy
compileOnly 'org.projectlombok:lombok:1.18.12'
annotationProcessor 'org.projectlombok:lombok:1.18.12'
```

### 5.2

Add a build step to write the ATP wallet files to the VM.

```yaml
- name: 'Write Wallet'
  uses: appleboy/ssh-action@master
  with:
    host: ${{ env.INSTANCE_IP }}
    username: opc
    key: ${{ secrets.VM_SSH_PRIVATE_KEY }}
    script: |
      sudo mkdir -p /wallet
      sudo mkdir -p /wallet-tmp
      sudo sh -c 'echo "${{secrets.WALLET_CWALLET}}" | base64 -d >> /wallet-tmp/cwallet.sso'
      sudo sh -c 'echo "${{secrets.WALLET_EWALLET}}" | base64 -d >> /wallet-tmp/ewallet.p12'
      sudo sh -c 'echo "${{secrets.WALLET_KEYSTORE}}" | base64 -d >> /wallet-tmp/keystore.jks'
      sudo sh -c 'echo "${{secrets.WALLET_OJDBC}}" | base64 -d >> /wallet-tmp/ojdbc.properties'
      sudo sh -c 'echo "${{secrets.WALLET_SQLNET}}" | base64 -d >> /wallet-tmp/sqlnet.ora'
      sudo sh -c 'echo "${{secrets.WALLET_TNSNAMES}}" | base64 -d >> /wallet-tmp/tnsnames.ora'
      sudo sh -c 'echo "${{secrets.WALLET_TRUSTSTORE}}" | base64 -d >> /wallet-tmp/truststore.jks'
      sudo mv /wallet-tmp/* /wallet/
```

### 5.3

Create an entity at `src/main/java/codes/recursive/domain/Person.java`.

```java
@Entity
@Table(name = "mn-oci-demo-persons-1")
@NoArgsConstructor
public class Person {
    @Id @GeneratedValue @Getter @Setter Long id;
    @Getter @Setter @Size(min = 1, max = 10) String firstName;
    @Getter @Setter @Size(min = 1, max = 10) String lastName;
    @Getter @Setter @Min(1L) @Max(125L) int age;
    @Getter @Setter @DateCreated Date dateCreated;
    @Getter @Setter @DateUpdated Date lastUpdated;

    public Person(@Size(min = 1, max = 10) String firstName, @Size(min = 1, max = 10) String lastName, @Min(1L) @Max(125L) int age) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
    }

}
```

### 5.4 

Create a repository at `src/main/java/codes/recursive/repository/PersonRepository.java`.

```java
package codes.recursive.repository;

import codes.recursive.domain.Person;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

@Repository
public interface PersonRepository extends CrudRepository<Person, Long> {}
```

### 5.5

Add CRUD methods to controller.

```java
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
```

### 5.6

Start app and test endpoints.

#### 5.6.1

Save a person:

```shell script
$ curl -X POST \
    -H "Content-Type: application/json" \
    -d '{"firstName": "Todd", "lastName": "Sharp", "age": 43}' \
    http://localhost:8080/hello/person | jq  
```

Returns:

```json
{
  "id": 67,
  "firstName": "Todd",
  "lastName": "Zharp",
  "age": 43,
  "dateCreated": 1589903508873,
  "lastUpdated": 1589903508873
}
```

#### 5.6.2

Save an invalid person:

```shell script
$ curl -X POST \
    -H "Content-Type: application/json" \
    -d '{"firstName": "Todd", "lastName": "Longlastname", "age": 143}' \
    http://localhost:8080/hello/person | jq  
```

Returns:

```json
{
  "message": "Bad Request",
  "_links": {
    "self": {
      "href": "/hello/person",
      "templated": false
    }
  },
  "_embedded": {
    "errors": [
      {
        "message": "entity.age: must be less than or equal to 125"
      },
      {
        "message": "entity.lastName: size must be between 1 and 10"
      }
    ]
  }
}
```

#### 5.6.3

Get a person by ID:

```shell script
$ curl http://localhost:8080/hello/person/67 | jq                                                                                                              
```

Returns:

```json
{
  "id": 67,
  "firstName": "Todd",
  "lastName": "Sharp",
  "age": 43,
  "dateCreated": 1589903508000,
  "lastUpdated": 1589903508000
}
```

#### 5.6.4

Get all persons:

```shell script
curl http://localhost:8080/hello/persons | jq  
```

Returns:

```json
[
  {
    "id": 64,
    "firstName": "T",
    "lastName": "Sharp",
    "age": 43,
    "dateCreated": 1589902358000,
    "lastUpdated": 1589902358000
  },
  {
    "id": 65,
    "firstName": "T",
    "lastName": "Sharp",
    "age": 43,
    "dateCreated": 1589903057000,
    "lastUpdated": 1589903057000
  }
]
```