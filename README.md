# Experiments with Temporal.io

## How to start it locally

- `git clone git@github.com:iLubenets/experiment-spring-temporal.git`
- run temporal.io cluster locally - temporal server & ui, postgres, elasticsearch
    - `cd docker-compose` - here we have `docker-compose.yml` which is copy from [Temporal Docker Compose](https://github.com/temporalio/docker-compose) but with some patches (marked with `#patch`)
    - `docker compose up -d`
- open temporal UI: [http://localhost:8081](http://localhost:8081)
- run spring boot app
    - from Intellij Idea: start spring-boot app `SpringTemporalApplication.java` (no specific profile needed)
    - from cli: `./gradlew bootRun`
- send API call to trigger WF: `api.http`
- check temporal UI
- change WF for your needs: `CreateRetailInvoiceWorkflowImpl.java`
