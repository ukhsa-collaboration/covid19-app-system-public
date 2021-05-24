'use strict';
const AWS = require('aws-sdk');
const sqs = new AWS.SQS({ apiVersion: '2012-11-05', maxRetries: 2, httpOptions: { timeout: 2000, connectTimeout: 2000 }});
exports.ingest = async event => {
    try {
        const result = await sqs.sendMessage({
            MessageBody: JSON.stringify(event),
            QueueUrl: process.env.TARGET_QUEUE,
        }).promise();
        console.log(result)
        return { statusCode: 200 };
    } catch (error) {
        console.error(JSON.stringify(error))
        return { statusCode: 500 };
    }
};
