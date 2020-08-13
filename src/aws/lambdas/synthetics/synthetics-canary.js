const synthetics = require('Synthetics');
const log = require('SyntheticsLogger');
const https = require('https');

const basicCustomEntryPoint = async function() {
  synthetics.setLogLevel(1); // 1 = info

  const fqdn = '${hostname}.' + '${base_domain}'
  const postData = JSON.stringify({
    startDate: "2020-07-28T01:00:00Z",
    endDate: "2020-07-28T22:00:00Z",
    postalDistrict: "SE1"
  });
  const options = {
    hostname: fqdn,
    path:     '${uri_path}',
    method:   '${method}',
    headers: {
      'User-Agent': '${hostname}-canary (Python https library)',
      'Accept': 'application/json'
    }
  };
  if('${auth_header}' !== '') {
    options.headers['Authorization'] = '${auth_header}';
  }
  if(options.method === 'POST' || options.method === 'PUT') {
    options.headers['Content-Type'] = 'application/json';
    options.headers['Content-Length'] = postData.length;
  }

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
    if(options.method === 'POST' || options.method === 'PUT') {
      req.write(postData);
    }
    req.end();
  });

  await promise
    .then(() => {return "Successfully completed basicCanary checks.";})
    .catch(err => {throw "Failed basicCanary check: " + err.message;});
};

exports.handler = async() => {
  return await basicCustomEntryPoint();
};
