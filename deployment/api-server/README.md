# Requirements
Vauhtijuoksu API requires two secrets in the cluster to function:
`vauhtijuoksu-api-htpasswd` and `vauhtijuoksu-api-psql`.

Create both by running:
`kubectl create secret generic <secret-name> --from-file <path-to-file>`

For htpasswd, the file should be a valid htpasswd file with name htpasswd.
For psql secrets, see kind-cluster/secret-conf.yaml for example.
