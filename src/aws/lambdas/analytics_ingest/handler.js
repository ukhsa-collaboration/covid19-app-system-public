'use strict';
const AWS = require('aws-sdk');
const sqs = new AWS.SQS({ apiVersion: '2012-11-05' });
exports.ingest = async event => {
    try {
        const clockPromise = new Promise((res) => setTimeout(() => res({ error: "Operation timed out", eventSize: JSON.stringify(event).length }), 20000));
        const resultPromise = sqs.sendMessage({
            MessageBody: JSON.stringify(event),
            QueueUrl: process.env.TARGET_QUEUE,
        }).promise();

        const result = await Promise.race([clockPromise, resultPromise])
        console.log(result)
        return { statusCode: 200 };
    } catch (error) {
        console.error(JSON.stringify(error))
        return { statusCode: 500 };
    }
};
