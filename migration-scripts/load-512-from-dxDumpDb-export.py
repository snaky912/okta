import json
import base64
import requests
from ldif3 import LDIFParser
from pprint import pprint

# This script servces as an example to import entries in LDIF into
# Universal Directory.  Note that this script only works with an
# export done by CA's dxDumpDb because of the way it encodes the
# userPassword one more time.

# Edit these variables based on the hash and org config
api_token = '004cSmv2L5UFSNoSOde3mkWpr0BkWBdzR1KJO6wKxf'
tenant = 'https://telemann2.oktapreview.com'
salt_order = 'POSTFIX'
hash_algorithm = 'SHA-512'

# Don't change these
url = '/api/v1/users'
request_headers = {
    'Accept': 'application/json',
    'Content-Type': 'application/json',
    'Authorization': 'SSWS ' + api_token
}

parser = LDIFParser(open('/Users/karmenlei/Downloads/dummytest45.ldif', 'rb'))
for dn, entry in parser.parse():
    print('got entry record: %s' % dn)
    pprint(entry['userPassword'][0])
    password_str = entry['userPassword'][0]
    password_len = len(password_str)
    # Need to strip off the {SSHA512} prefix
    password_str = password_str[9:password_len]
    print('Revised password value: ' + password_str)
    password_base64_decoded = base64.b64decode(password_str)
    # Password length of a SSHA512 hash is always 64 bytes long
    # Subtract 64 from the hash will give the length of the salt
    salt = password_base64_decoded[64:len(password_base64_decoded)]
    print('salt:' + salt.hex())
    print('salt base64 encoded: ' + base64.b64encode(salt).decode("utf-8"))
    password_hash = password_base64_decoded[0:64]
    print('password hash:' + password_hash.hex())
    print('password base64 encoded: ' + str(base64.b64encode(password_hash)))
    # populate the JSON payload with the user profile
    payload = {
                "profile": {
                    "firstName": entry['givenName'][0],
                    "lastName": entry['sn'][0],
                    "email": entry['mail'][0],
                    "login": entry['mail'][0],
                    "userType": "tfsperson",
                    "tmssguid": entry['tmssguid'][0],
                    "tmsTBGResourceEnrollmentStatus": entry['tmsTBGResourceEnrollmentStatus'][0]
                 },
                 # May or may not need a custom user type,
                 # Just want to demonstrate that it's feasible
                 # during user creation
                 "type": {
                    "id": "otyu6tusmlyLGl4Kg0h7"
                 },
                "credentials": {
                    "password" : {
                        "hash": {
                        "algorithm": hash_algorithm,
                        "salt": base64.b64encode(salt).decode("utf-8"),
                        "saltOrder": salt_order,
                        "value": base64.b64encode(password_hash).decode("utf-8")
                        }
                    }
                }
    }

    #For debugging, print out the complete json response.
    #print(json.dumps(json_response,indent=2))
    print(json.dumps(payload,indent=3))
    r = requests.post(tenant + url, headers=request_headers ,json=payload)
    if (r.status_code == 200):
        print('User ' + entry['mail'][0] + ' created.')
        # Now add the email MFA factor
        json_response = r.json()
        uuid = json_response['id']
        print('user uuid: ' + uuid)
        payload = {
                    "factorType": "email",
                    "provider": "OKTA",
                    "profile": {
                          "email": entry['mail'][0]
                }
        }
        r = requests.post(tenant + url + '/' + uuid + '/factors', headers=request_headers ,json=payload)
        if (r.status_code == 200):
            print('Email factor enrolled but still needs activation.')
        else:
            json_response = r.json()
            print('Error: ' + str(r.status_code) + ' "' + json_response['errorSummary'] + '" Cause: ' + str(json_response['errorCauses']))
    else:
        json_response = r.json()
        print('Error: ' + str(r.status_code) + ' "' + json_response['errorSummary'] + '" Cause: ' + str(json_response['errorCauses']))
