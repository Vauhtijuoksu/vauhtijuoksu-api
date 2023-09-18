# Vauhtijuoksu API server

## Requirements
These are required to run all the commands mentioned here. 
Some commands might work without all of these if you're an adventurous person.
* java 17 - Install Java and point JAVA_HOME environment variable to your installation directory
* jq
* kubectl 1.25
* helm 3
* kind 0.17.0
* bash

## API documentation
### Generating the API documentation
```shell
./gradlew :api-doc:build
```
The api is readable after build at [api-doc/build/swagger-ui/index.html](api-doc/build/swagger-ui/index.html)

### Browsing the API documentation
The latest API documentation generated from main branch is available for browsing at https://static.vauhtijuoksu.fi

### Testing against the API specification
A mock server using the API specification (from `main` branch) is served at https://mockapi.dev.vauhtijuoksu.fi/.
Note that since / has no content, the server correctly responds 404.

The mock server can also be deployed locally by running:
```shell
./gradlew localMockApi
```

It's then available at http://mockapi.localhost

### Using the API
The API server is running at https://api.dev.vauhtijuoksu.fi. Ask around for credentials if you need to modify data.

## Development
Run tests with:
```shell
./gradlew test -x feature-tests:test
```
This skips the slow feature-tests.

To run latest api server locally:
```shell
./gradlew runInCluster
```
The server is accessible at http://api.localhost. 
It's also available with https, using a self-signed certificate. 
The certificate must be trusted or ignored to access the server.
The command can be run again to update the deployment to the latest version.
Default credentials for the api: `vauhtijuoksu`:`vauhtijuoksu`

The cluster can be deleted with:
```shell
./gradlew tearDownCluster
```

### Testing OAuth on the local cluster
Copy `deployment/kind-cluster/oauth-secret-template.yaml` withouth the `-template` suffix and copy the values from
Discord developer site.

## Feature tests

Feature tests are run against a local kind cluster by running at project root
```shell
./gradlew build
``` 

The tests generate a Jacoco report file, which can be found at
[build/reports/jacoco/featureTestReport/html/index.html](build/reports/jacoco/featureTestReport/html/index.html).

## Versions
Get current version by running
```shell
./scripts/version.sh
```

Versions are generated from the latest annotated git tag.
Feel free to create new tags with `git tag` whenever necessary.
Example:
```shell
git tag -am 0.1 0.1 9de10436d1c097826845679cda1633c90f968fc1
git push origin 0.1
```
