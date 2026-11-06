# Deploy TraderX Using Radius

Radius can be used to deploy and run the [TraderX](https://github.com/finos/traderX) application.

## Prerequisites

- Install and set up [Radius](https://docs.radapp.io/getting-started/) and local Kubernetes cluster
- Optionally set up an [Azure](https://docs.radapp.io/providers/azure/) or [AWS](https://docs.radapp.io/providers/aws/) provider and environment in Radius

## Instructions

To deploy the TraderX app using Radius, you can run the commands directly in this directory.

First, initialize Radius:
```bash
rad init
```

>Select `Yes` when prompted to setup an application in the current directory

account-service requires HTTP Basic credentials: trade-service reads accounts with the reader user, and creating or renaming accounts needs the admin user. Pass random passwords for both at deploy time (they are `@secure()` parameters and are not committed):
```bash
export ACCOUNT_READER_PW=$(openssl rand -hex 24) ACCOUNT_ADMIN_PW=$(openssl rand -hex 24)
```

To deploy and run the app in your local environment:
```bash
rad run traderx.bicep --parameters accountServiceReaderPassword=$ACCOUNT_READER_PW accountServiceAdminPassword=$ACCOUNT_ADMIN_PW
```

If you have an Azure environment set up, you can deploy the app there as well (in this example the Azure environment is associated with the `prod-azure` workspace):
```bash
rad deploy traderx.bicep -w prod-azure --parameters accountServiceReaderPassword=$ACCOUNT_READER_PW accountServiceAdminPassword=$ACCOUNT_ADMIN_PW
```

If you have an AWS environment set up, you can deploy the app there as well (in this example the AWS environment is associated with the `prod-aws` workspace):
```bash
rad deploy traderx.bicep -w prod-aws --parameters accountServiceReaderPassword=$ACCOUNT_READER_PW accountServiceAdminPassword=$ACCOUNT_ADMIN_PW
```