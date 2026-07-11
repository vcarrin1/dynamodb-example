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
- delete customer (cascades to related orders and payments)
- delete order
- delete payment
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

## Security with Auth0

This project uses Auth0 for OAuth2 JWT-based authentication and authorization.

### Configuration

Security is configured in [src/main/java/com/vcarrin87/dynamodb_example/config/SecurityConfig.java](src/main/java/com/vcarrin87/dynamodb_example/config/SecurityConfig.java) with:

- **Issuer URI**: OAuth2 token issuer (e.g., `https://dev-uiyohlg5bc7ywx10.us.auth0.com/`)
- **Audience**: Expected audience claim in token (e.g., `demo-authorization-api`)

Update these values in `src/main/resources/application.yaml` under `spring.security.oauth2.resourceserver.jwt`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://YOUR_AUTH0_DOMAIN/
          audience: YOUR_API_IDENTIFIER
```

### JWT Validation

The JwtDecoder validates:
1. Token signature using issuer's public key
2. Token issuer matches configured issuer-uri
3. Audience claim contains configured audience value

Invalid or missing tokens return HTTP 401 with JSON error:
```json
{"errors":[{"message":"Unauthorized"}]}
```

### Permissions and Authorization

Permissions from the Auth0 token `permissions` claim are mapped to Spring Security authorities with `SCOPE_` prefix.

For example, Auth0 permission `ADMIN` becomes Spring authority `SCOPE_ADMIN`.

Protected endpoints use `@PreAuthorize` annotations:
- `@PreAuthorize("hasAuthority('SCOPE_ADMIN')")` — requires ADMIN permission
- `@PreAuthorize("hasAnyAuthority('SCOPE_ADMIN', 'SCOPE_USER')")` — requires ADMIN or USER

Requests with valid token but insufficient permissions return HTTP 403 with JSON error:
```json
{"errors":[{"message":"Forbidden"}]}
```

### Testing with Auth0 Tokens

1. Get an access token from your Auth0 tenant (e.g., via Authorization Code flow or M2M client credentials)
2. In GraphiQL, click the "Headers" tab
3. Add:
   ```json
   {
     "Authorization": "Bearer YOUR_ACCESS_TOKEN"
   }
   ```
4. Run your query

The token is validated on each request. If it contains the required permissions, the query executes.

(Note: Security is currently configured to require Auth0 by default.)

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
In GraphiQL, the example queries are available from the collapsible examples tab.

## Example GraphQL Operations

Create customer:

<details>
<summary>Show create customer mutation</summary>

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

</details>

Create order:

<details>
<summary>Show create order mutation</summary>

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

</details>

Create payment:

<details>
<summary>Show create payment mutation</summary>

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

</details>

Delete customer (cascades related orders/payments):

<details>
<summary>Show delete customer mutation</summary>

```graphql
mutation {
  deleteCustomer(customerId: "11111111-1111-1111-1111-111111111111")
}
```

</details>

Delete order:

<details>
<summary>Show delete order mutation</summary>

```graphql
mutation {
  deleteOrder(
    orderId: "22222222-2222-2222-2222-222222222222"
    customerId: "11111111-1111-1111-1111-111111111111"
  )
}
```

</details>

Delete payment:

<details>
<summary>Show delete payment mutation</summary>

```graphql
mutation {
  deletePayment(
    paymentId: "33333333-3333-3333-3333-333333333333"
    orderId: "22222222-2222-2222-2222-222222222222"
  )
}
```

</details>

List customers with filter + pagination:

<details>
<summary>Show customers query with pagination</summary>

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

</details>

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
