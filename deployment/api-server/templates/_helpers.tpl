{{/*
Expand the name of the chart.
*/}}
{{- define "vauhtijuoksu-api.name" -}}
{{- .Chart.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "vauhtijuoksu-api.fullname" -}}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "vauhtijuoksu-api.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "vauhtijuoksu-api.labels" -}}
helm.sh/chart: {{ include "vauhtijuoksu-api.chart" . }}
{{ include "vauhtijuoksu-api.selectorLabels" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "vauhtijuoksu-api.selectorLabels" -}}
app.kubernetes.io/name: {{ include "vauhtijuoksu-api.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
