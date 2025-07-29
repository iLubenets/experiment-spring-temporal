# For local use - presentation only

Source taken from: https://github.com/temporalio/docker-compose

# URLs
- UI http://localhist:8081

# Admin tools
```bash
# Access the admin tools container
docker exec -it temporal-admin-tools bash

# Once inside, you can use temporal CLI commands:
temporal workflow list
temporal workflow describe --workflow-id <workflow-id>
temporal workflow terminate --workflow-id <workflow-id>

# Run commands directly without entering the container
docker exec temporal-admin-tools temporal workflow list

# Examples:
# List workflows
docker exec temporal-admin-tools temporal workflow list --namespace default

# Describe a workflow
docker exec temporal-admin-tools temporal workflow describe \
  --workflow-id CreateIndividualInvoiceWorkflow \
  --namespace default

# Show workflow history
docker exec temporal-admin-tools temporal workflow show \
  --workflow-id CreateIndividualInvoiceWorkflow \
  --namespace default

# Create a new namespace
docker exec temporal-admin-tools temporal namespace create \
  --name my-namespace \
  --retention 7d

# Update dynamic config
docker exec temporal-admin-tools temporal admin config set \
  --key frontend.enableServerVersionCheck \
  --value false
```

# Docker Commands
```bash
# start
docker compose up -d
# start
docker compose restart
# stop
docker compose down
```

# Versions
Set version in `.env`.

Tack version updates:
- UI Web: https://github.com/temporalio/ui/releases
- UI Server: https://github.com/temporalio/ui-server/releases
- Temporal: https://github.com/temporalio/temporal/releases
- Admin Tool (CLI): https://hub.docker.com/r/temporalio/admin-tools/tags
