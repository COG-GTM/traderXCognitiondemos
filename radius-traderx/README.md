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

`app.bicep` requires two secure parameters, `tradeFeedTradeServiceToken` and `tradeFeedTradeProcessorToken` (at least 32 random characters each, e.g. `openssl rand -hex 32`), which the trade-feed uses to authenticate trade-service and trade-processor. Pass them with `--parameters` and keep them out of source control.

To deploy and run the app in your local environment:
```bash
rad run traderx.bicep
```

If you have an Azure environment set up, you can deploy the app there as well (in this example the Azure environment is associated with the `prod-azure` workspace):
```bash
rad deploy traderx.bicep -w prod-azure
```

If you have an AWS environment set up, you can deploy the app there as well (in this example the AWS environment is associated with the `prod-aws` workspace):
```bash
rad deploy traderx.bicep -w prod-aws
```