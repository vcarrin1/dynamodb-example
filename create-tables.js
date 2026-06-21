#!/usr/bin/env node
const { spawnSync } = require('child_process');

const endpointUrl = 'http://localhost:8000';
const region = 'us-west-1';
const env = { ...process.env, AWS_ACCESS_KEY_ID: 'dummy', AWS_SECRET_ACCESS_KEY: 'dummy' };

const tables = [
  'CustomerTable',
  'ProductTable',
  'InventoryTable',
  'OrderTable',
  'OrderLineTable',
  'PaymentTable',
];

function runAws(args) {
  const result = spawnSync('aws', args, { stdio: 'inherit', env });
  if (result.error) {
    console.error('Unable to run aws command:', result.error.message);
    process.exit(1);
  }
  if (result.status !== 0) {
    process.exit(result.status);
  }
}

function tableExists(tableName) {
  const result = spawnSync(
    'aws',
    [
      'dynamodb',
      'describe-table',
      '--table-name',
      tableName,
      '--endpoint-url',
      endpointUrl,
      '--region',
      region,
    ],
    { stdio: 'ignore', env }
  );
  return result.status === 0;
}

for (const tableName of tables) {
  console.log(`\n=== ${tableName} ===`);

  if (tableExists(tableName)) {
    console.log(`Table ${tableName} already exists, skipping.`);
    continue;
  }

  runAws([
    'dynamodb',
    'create-table',
    '--table-name',
    tableName,
    '--attribute-definitions',
    'AttributeName=PKey,AttributeType=S',
    'AttributeName=SKey,AttributeType=S',
    '--key-schema',
    'AttributeName=PKey,KeyType=HASH',
    'AttributeName=SKey,KeyType=RANGE',
    '--billing-mode',
    'PAY_PER_REQUEST',
    '--endpoint-url',
    endpointUrl,
    '--region',
    region,
  ]);

  console.log(`Created table ${tableName}.`);
}

console.log('\nAll tables processed.');
