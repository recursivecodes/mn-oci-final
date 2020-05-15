# mn-oci-final

## Step 1 - Create App

See branch step 1: https://github.com/recursivecodes/mn-oci-final/tree/step-1

### 1.1

Create an app with the CLI.

```bash
mn create-app codes.recursive.mn-oci --features data-jpa --jdk 11
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

### 2.2

Populate `oracle-cloud.yaml`.

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

      - name: 'Get Version Number'
        run: |
          echo "::set-env name=VERSION::$(./gradlew properties -q | grep "version:" | awk '{print $2}')"

      - name: 'Publish JAR'
        uses: actions/upload-artifact@v2-preview
        with:
          name: 'mn-oci-${{env.VERSION}}-all.jar'
          path: build/libs/*-all.jar

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

      - name: 'Get Instance IP'
        run: |
          echo "::set-env name=INSTANCE_IP::$( \
            oci compute instance list-vnics \
            --instance-id ${{env.INSTANCE_OCID}} \
            --query 'data [0]."public-ip"' \
            --raw-output \
          )"
          echo "1"

      - name: 'Wait for SSH'
        run: |
          while ! nc -w5 -z ${{ env.INSTANCE_IP }} 22; do
                  sleep 5
                  echo "SSH not available..."
          done; echo "SSH ready!"

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

      - name: 'Push JAR'
        uses: appleboy/scp-action@master
        with:
          host: ${{ env.INSTANCE_IP }}
          username: opc
          key: ${{ secrets.VM_SSH_PRIVATE_KEY }}
          source: "build/libs/mn-oci-${{env.VERSION}}-all.jar"
          target: "app"
          strip_components: 2

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

## Step 3 - Configure App & Dependencies

