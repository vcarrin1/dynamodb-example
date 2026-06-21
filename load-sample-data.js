#!/usr/bin/env node
const fs = require('fs');
const os = require('os');
const path = require('path');
const { spawnSync } = require('child_process');

const endpointUrl = 'http://localhost:8000';
const region = 'us-west-1';
const env = { ...process.env, AWS_ACCESS_KEY_ID: 'dummy', AWS_SECRET_ACCESS_KEY: 'dummy' };
const samplePath = path.resolve(__dirname, 'sample-items.json');

if (!fs.existsSync(samplePath)) {
  console.error('sample-items.json not found in the project root.');
  process.exit(1);
}

const sampleData = JSON.parse(fs.readFileSync(samplePath, 'utf8'));
const requests = [];
for (const [tableName, records] of Object.entries(sampleData)) {
  for (const record of records) {
    requests.push({ tableName, record });
  }
}

function runAws(args) {
  const result = spawnSync('aws', args, { env, encoding: 'utf8' });
  if (result.error) {
    throw result.error;
  }
  if (result.status !== 0) {
    throw new Error(result.stderr || `aws exited with code ${result.status}`);
  }
  return result.stdout;
}

function writeBatch(requestItems) {
  const payload = requestItems;
  const tmpFile = path.join(os.tmpdir(), `sample-items-${Date.now()}-${Math.random().toString(36).slice(2)}.json`);
  fs.writeFileSync(tmpFile, JSON.stringify(payload, null, 2));

  try {
    const output = runAws([
      'dynamodb',
      'batch-write-item',
      '--request-items',
      `file://${tmpFile}`,
      '--endpoint-url',
      endpointUrl,
      '--region',
      region,
      '--output',
      'json',
    ]);

    return output.trim() ? JSON.parse(output).UnprocessedItems || {} : {};
  } finally {
    fs.unlinkSync(tmpFile);
  }
}

let currentBatch = {};
let batchCount = 0;
let batchItemCount = 0;

function flushBatch() {
  if (batchItemCount === 0) {
    return;
  }

  console.log(`Writing batch with ${batchItemCount} items...`);
  let unprocessed = writeBatch(currentBatch);
  let attempt = 1;

  while (Object.keys(unprocessed).length > 0) {
    const unprocessedCount = Object.values(unprocessed).reduce((sum, items) => sum + items.length, 0);
    console.log(`  Retrying ${unprocessedCount} unprocessed items (attempt ${attempt})...`);
    currentBatch = unprocessed;
    batchItemCount = unprocessedCount;
    unprocessed = writeBatch(currentBatch);
    attempt += 1;
    if (attempt > 5) {
      throw new Error('Too many retries for unprocessed items.');
    }
  }

  currentBatch = {};
  batchItemCount = 0;
  batchCount += 1;
}

for (const { tableName, record } of requests) {
  currentBatch[tableName] = currentBatch[tableName] || [];
  currentBatch[tableName].push(record);
  batchItemCount += 1;

  if (batchItemCount >= 25) {
    flushBatch();
  }
}

flushBatch();
console.log(`\nLoaded ${requests.length} sample items into DynamoDB.`);
