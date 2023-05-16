#!/usr/bin/env bash

helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

helm install -f helm-config/order-db-values.yaml order-db bitnami/cassandra
helm install -f helm-config/stock-db-values.yaml stock-db bitnami/cassandra
helm install -f helm-config/payment-db-values.yaml payment-db bitnami/cassandra