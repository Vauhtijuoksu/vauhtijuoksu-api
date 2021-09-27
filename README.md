# Vauhtijuoksu API

## Requirements
* java 16
* helm 3
* jq

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
