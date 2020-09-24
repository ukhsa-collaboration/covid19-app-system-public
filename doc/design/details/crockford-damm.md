# Crockford Damm base 32 codes

Base32 encoded 40byte random number, using the Crockford encoding.
Javascript for validation is attached below.

## Javascript to validate Token

Taken fom the V2 documentation

```javascript
function validateAppRefCode(code) {
  var CROCKFORD_BASE32 = "0123456789abcdefghjkmnpqrstvwxyz";
  var cleaned = code.toLowerCase().replace(/il/g, "1").replace(/o/g, "0").replace(/u/g, "v").replace(/[- ]/g, "");
  var i;
  var checksum = 0;
  var digit;

  for (i = 0; i < cleaned.length; i++) {
    digit = CROCKFORD_BASE32.indexOf(cleaned.charAt(i));
    checksum = damm32(checksum, digit);
  }

  return checksum == 0;
}

function damm32(checksum, digit) {
  var DAMM_MODULUS = 32;
  var DAMM_MASK = 5;

  checksum ^= digit;
  checksum *= 2;
  if (checksum >= DAMM_MODULUS) {
    checksum = (checksum ^ DAMM_MASK) % DAMM_MODULUS;
  }
  return checksum;
}
```
