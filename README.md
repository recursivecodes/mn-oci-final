# mn-oci-final

## Step 1 - Create App

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

### 2.1