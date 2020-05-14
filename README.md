# mn-oci-final

## Step 1 - Create App

See branch step 1: https://github.com/recursivecodes/mn-oci-final/tree/step-1

### 1.1

Create an app with the CLI.

```bash
mn create-app codes.recursive.mn-oci-final --features data-jpa --jdk 11
```

### 1.2

Add a controller.

```bash
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

```bash 
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
| ------------------------- |
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

### 2.1

Add `.github/workflows/oracle-cloud.yaml`