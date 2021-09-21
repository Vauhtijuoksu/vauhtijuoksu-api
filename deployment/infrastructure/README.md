# DNS + TLS
NOTE: I didn't test these on fresh installation after a few try and errors. Versions aren't tagged so something might break in the future.

Created by following and improvising, based on Microsoft tutorials here: https://docs.microsoft.com/en-us/azure/aks/ingress-tls

```shell
DNS_LABEL=dev.vauhtijuoksu.fi

helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm install nginx-ingress ingress-nginx/ingress-nginx \
    --set controller.replicaCount=1 \
    --set controller.nodeSelector."kubernetes\.io/os"=linux \
    --set controller.admissionWebhooks.patch.nodeSelector."kubernetes\.io/os"=linux \
    --set defaultBackend.nodeSelector."kubernetes\.io/os"=linux \
    --set controller.service.annotations."service\.beta\.kubernetes\.io/azure-dns-label-name"=$DNS_LABEL

helm repo add jetstack https://charts.jetstack.io
helm install cert-manager jetstack/cert-manager \
--set installCRDs=true \
--set nodeSelector."kubernetes\.io/os"=linux

kubectl apply -f cert-issuer.yaml
```
Get newly created IP by running:
```shell
kubectl get service/nginx-ingress-ingress-nginx-controller
```
Configure DNS records to point at the IP

Apply services with ingress. See mockserver for example.
