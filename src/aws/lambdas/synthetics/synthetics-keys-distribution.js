const synthetics = require('Synthetics');
const log = require('SyntheticsLogger');
const https = require('https');

/**
 * Generates zip file name for incremental distribution based on current date
 * @returns name of zip file, e.g. 2020080800.zip at any time on 8 August 2020
 */
const generate_file_name = function() {
  const today = new Date();
  return today.toISOString().slice(0,10).replace(/-/g,"") + "00.zip";
}

const basicCustomEntryPoint = async function() {
  synthetics.setLogLevel(1); // 1 = info

  const fqdn = '${hostname}.' + '${base_domain}'
  const options = {
    hostname: fqdn,
    path:     '${uri_path}/' + generate_file_name(),
    method:   '${method}',
    headers: {
      'User-Agent': '${hostname}-canary (Python https library)',
      'Authorization': '${auth_header}',
      'Accept': 'application/json'
    }
  };

  let promise = new Promise((resolve, reject) => {
    log.info('Sending HTTPs request with options ', JSON.stringify(options));
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
