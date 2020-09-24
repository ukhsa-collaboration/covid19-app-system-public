const synthetics = require('Synthetics');
const log = require('SyntheticsLogger');
const https = require('https');
const AWS = require('aws-sdk');

// Create a Secrets Manager client
const secretsManager = new AWS.SecretsManager(); // use default region - where this canary is deployed

/**
 * Retrieve the authorization header value from the AWS secrets manager
 */
const get_auth_header = async function() {
  let promise = new Promise((resolve, reject) => {
    log.debug('Calling secretsManager.getSecretValue({SecretId: "${secret_name}"})');
    return secretsManager.getSecretValue({SecretId: '${secret_name}'}, (err, data) => {
      if (err) {
        log.error('secretsManager.getSecretValue({SecretId: "${secret_name}"}) failed:', err.code);
        reject(err);
      }
      else {
        log.debug('secretsManager.getSecretValue({SecretId: "${secret_name}"}) returned:', JSON.stringify(data));
        resolve(data);
      }
    });
  });

  return await promise
    .then(secret_data => {
      if ('SecretString' in secret_data) {
        return secret_data.SecretString;
      } else {
        throw "Auth header not found in AWS secret";
      }
    });
}

/**
 * Generates zip file name for incremental distribution based on current date
 * @returns name of zip file, e.g. 2020080800.zip at any time on 8 August 2020
 */
const generate_file_name = function() {
  const today = new Date();
  return today.toISOString().slice(0,10).replace(/-/g,"") + "00.zip";
}

/**
 * Convert number to byte array
 * Adapted from https://stackoverflow.com/questions/8482309/converting-javascript-integer-to-byte-array-and-back
 * @param longNumber
 * @returns {number[]}
 */
longToByteArray = function(/*long*/longNumber) {
  // we want to represent the input as a 8-bytes array
  var byteArray = [0, 0, 0, 0, 0, 0, 0, 0];

  for(var index = byteArray.length; index > 0; ) {
    const byte = longNumber & 0xff;
    byteArray[--index] = byte;
    longNumber = (longNumber - byte) / 256;
  }

  return byteArray;
};

/**
 * Generate a partly-random request-id HTTP header value
 * @returns a string consisting of a synthetic canary identifier and a random string based on the time
 */
const generate_request_id = function() {
  const epoch_ms = new Date().valueOf();
  const buff = Buffer.from(longToByteArray(epoch_ms));
  return 'cwsyn-' + buff.toString('base64');
}

const basicCustomEntryPoint = async function() {
  synthetics.setLogLevel(1); // 1 = info

  const authHeader = ('${secret_name}' == '') ? null : await get_auth_header();
  const fqdn = '${hostname}.' + '${base_domain}'
  const options = {
    hostname: fqdn,
    path:     '${uri_path}/' + generate_file_name(),
    method:   '${method}',
    headers: {
      'User-Agent': '${hostname}-canary (node.js https library)',
      'Request-Id': generate_request_id(),
      'Accept': 'application/json'
    }
  };
  if(authHeader) {
    options.headers['Authorization'] = '****************'; // for logging purposes
  }

  let promise = new Promise((resolve, reject) => {
    log.info('Sending HTTPs request with options ', JSON.stringify(options));
    if(authHeader) {
      options.headers['Authorization'] = authHeader;
    }
    let req = https.request(options, (resp) => {
      let data = '';

      const statusCode = resp.statusCode;
      if(statusCode.toString() !== '${expc_status}') {
        if ((statusCode < 200) || (statusCode > 299)) {
          // Can't use templating syntax here because it confuses Terraform's templatefile function
          reject(new Error(statusCode.toString() + ' ' + resp.statusMessage));
        }
      }

      resp.on('error', err => {
        reject(err);
      })

      resp.on('timeout', () => {
        reject("timeout");
      })

      // A chunk of data has been received.
      resp.on('data', (chunk) => {
        data += chunk;
      });

      // The whole response has been received. Print out the result.
      resp.on('end', () => {
        log.info('Returned data:', data);
        resolve(data);
      });

    }).on('error', (err) => {
      log.error('Error:', err.message);
      reject(err);
    });
    req.end();
  });

  await promise
    .then(() => {return "Successfully completed basicCanary checks.";})
    .catch(err => {throw "Failed basicCanary check: " + err.message;});
};

exports.handler = async() => {
  return await basicCustomEntryPoint();
};
