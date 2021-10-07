# Vauhtijuoksu API

## Requirements
* java 16
* jq
* kubectl 1.20
* helm 3
* kind
* bash

## Generating API
```shell
./gradlew build
```
The api is readable after build at api-doc/build/swagger-ui/index.html

## Browsing the API specification
The latest API from master is available at https://static.vauhtijuoksu.fi

## Testing the API
A mock server using the API specification (from `main` branch) is served at https://dev.vauhtijuoksu.fi/. 
Note that since / has no content, the server correctly responds 404.

## Using the API
The API server is running at https://api.dev.vauhtijuoksu.fi

## Development
To run latest api server locally:
```shell
./gradlew runInCluster
```
The server is accessible at https://localhost. The certificate is self-signed and must be trusted or ignored 
to access the server.

Note that the server does not restart at the moment on code changes, since the images have the same version.

The cluster can be deleted with:
```shell
./gradlew tearDownCluster
```
