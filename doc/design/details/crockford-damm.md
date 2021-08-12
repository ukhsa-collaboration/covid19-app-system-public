# Crockford-Damm base 32 code

[Crockford Base32](https://www.crockford.com/base32.html) is an encoding scheme designed to be compact, human readable, pronouncable and error resistant.

A 5 byte random number is encoded as 8 characters using the [Crockford Base32](https://www.crockford.com/base32.html) encoding, and the [Damm](https://en.wikipedia.org/wiki/Damm_algorithm) check-digit algorithm.


## Validation (Javascript)

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
