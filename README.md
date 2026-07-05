# DynamoDB Example (Spring Boot + GraphQL)

This project is a local-first reference app that demonstrates how to build a GraphQL API on top of Amazon DynamoDB using Spring Boot.

It models a simple commerce domain with customers, orders, and payments, and shows:

- single-table style key patterns for customer data
- GraphQL queries and mutations with custom scalar support (`UUID`)
- pagination with `nextToken`
- optional filtering on customer list queries
- local DynamoDB development with seed scripts

## Tech Stack

- Java 25
- Spring Boot 3.5
- Spring for GraphQL
- AWS SDK v2 DynamoDB Enhanced Client
- Docker + DynamoDB Local

## Domain Overview

The API supports these core entities:

- `CustomerItem`
- `OrderItem`
- `PaymentItem`

Key API capabilities include:

- upsert customer profile
- create order
- create payment
- fetch customer aggregate (`customer + orders + payments`)
- list customers with pagination and optional filters (`customerId`, `createdAt`, `name contains`)

ID type notes:

- `customerId`, `orderId`, `paymentId`, `orderItemId`, and `productId` are GraphQL `UUID` fields.
- For mutation inputs, `orderId` and `paymentId` are optional UUIDs. If omitted, the server generates them.

## Run Locally

Start DynamoDB Local:

```bash
docker compose -f docker-compose.yaml up -d
```

Start the Spring Boot app:

```bash
./mvnw spring-boot:run
```

## Seed Local Data

This repository includes helper scripts at the root:

- `create-tables.js`: create DynamoDB tables
- `load-sample-data.js`: load sample records from `sample-items.json`

Run in order:

```bash
node create-tables.js
node load-sample-data.js
```

Requirements:

- Node.js
- AWS CLI (used by setup scripts)

Both scripts target `http://localhost:8000` with dummy credentials.

## Optional: Create CustomerId GSI

If you want faster customer order lookups through a GSI:

```bash
AWS_ACCESS_KEY_ID=dummy AWS_SECRET_ACCESS_KEY=dummy \
aws dynamodb update-table \
  --table-name CustomerTable \
  --attribute-definitions AttributeName=CustomerId,AttributeType=S \
  --global-secondary-index-updates '[{"Create":{"IndexName":"CustomerIdIndex","KeySchema":[{"AttributeName":"CustomerId","KeyType":"HASH"}],"Projection":{"ProjectionType":"ALL"},"OnDemandThroughput":{"MaxReadRequestUnits":40,"MaxWriteRequestUnits":40}}}]' \
  --endpoint-url http://localhost:8000 \
  --region us-west-1
```

## GraphQL Endpoint

- URL: `http://localhost:3000/graphiql`

If security is enabled in your local environment, include a valid Bearer token in your requests.

## Example GraphQL Operations

Create customer:

```graphql
mutation {
  customerUpsert(
    customer: {
      name: "Jane Doe"
      email: "jane.doe@example.com"
      address: "123 Main St"
    }
  ) {
    customerId
    name
    createdAt
  }
}
```

Create order:

```graphql
mutation {
  createOrder(
    order: {
      customerId: "11111111-1111-1111-1111-111111111111"
      orderStatus: "CREATED"
      deliveryDate: "2026-07-01T00:00:00Z"
    }
  ) {
    orderId
    customerId
    orderStatus
  }
}
```

Create payment:

```graphql
mutation {
  createPayment(
    payment: {
      orderId: "22222222-2222-2222-2222-222222222222"
      paymentMethod: "CARD"
      amount: 49.99
      paymentDate: "2026-07-01T12:00:00Z"
    }
  ) {
    paymentId
    orderId
    paymentMethod
    amount
  }
}
```

List customers with filter + pagination:

```graphql
query Customers($pageSize: Int, $nextToken: String, $filter: CustomerFilterInput) {
  customers(pageSize: $pageSize, nextToken: $nextToken, filter: $filter) {
    items {
      customerId
      name
      createdAt
    }
    nextToken
  }
}
```

Variables example:

```json
{
  "pageSize": 10,
  "nextToken": null,
  "filter": {
    "name": "Jane"
  }
}
```

## Inspect DynamoDB in Browser

```bash
npm install -g dynamodb-admin
export DYNAMO_ENDPOINT=http://localhost:8000
dynamodb-admin
```

Then open `http://localhost:8001`.
