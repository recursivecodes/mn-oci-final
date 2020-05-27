@Grapes([
        @Grab(group='com.muquit.libsodiumjna', module='libsodium-jna', version='1.0.4'),
        @Grab(group='net.java.dev.jna', module='jna', version='5.5.0'),
        @Grab(group='org.slf4j', module='slf4j-api', version='1.7.30')
])

import groovy.json.JsonSlurper
import groovy.util.*
import com.muquit.libsodiumjna.SodiumLibrary
import org.slf4j.*

/*
call this script like so:
groovy [this script name] [name of groovy properties file containing secrets] [owner] [repo]

for example:
groovy create-secrets.groovy props.groovy recursivecodes secret-test

see props.groovy in this directory!
*/
def props = args[0]
def owner = args[1]
def repo = args[2]

/* load config */
ConfigObject conf = new ConfigSlurper().parse(new File(props).toURI().toURL())
Map config = conf.get('config')
def token = config.get('token')
def slurper = new JsonSlurper()

/* get public key */
def cmd1 = ["bash", "-c", "curl -H 'Authorization: token ${token}' https://api.github.com/repos/${owner}/${repo}/actions/secrets/public-key"]
def proc1 = cmd1.execute()
proc1.waitFor()
def keyObj = slurper.parseText(proc1.text)

/* depends on libsodium on file system : brew install libsodium */
SodiumLibrary.setLibraryPath("/usr/local/Cellar/libsodium/1.0.18_1/lib/libsodium.dylib")

conf.get('secrets').each { String secretName, String secretValue ->
    def value = secretValue.bytes
    def decodedKey = keyObj.key.decodeBase64()
    def encryptedValue = SodiumLibrary.cryptoBoxSeal(value, decodedKey).encodeBase64()
    def createSecretCmd = [
            "bash",
            "-c",
            "curl -H 'Authorization: token ${token}' -X PUT -d '{\"encrypted_value\" : \"${encryptedValue.toString()}\", \"key_id\": \"${keyObj.key_id}\"}' https://api.github.com/repos/${owner}/${repo}/actions/secrets/${secretName}"
    ]
    def proc = createSecretCmd.execute()
    proc.waitFor()
    println "Created/Updated `$secretName`"
}