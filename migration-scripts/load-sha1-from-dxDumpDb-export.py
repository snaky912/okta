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
hash_algorithm = 'SHA-1'

# Don't change these
url = '/api/v1/users'
request_headers = {
    'Accept': 'application/json',
    'Content-Type': 'application/json',
    'Authorization': 'SSWS ' + api_token
}

parser = LDIFParser(open('/Users/karmenlei/Downloads/sha1.entry.ldif', 'rb'))
for dn, entry in parser.parse():
    print('got entry record: %s' % dn)
    print(entry['userPassword'][0])
    password_str = entry['userPassword'][0]
    # strip off the {SHA} prefix
    password_len = len(password_str)
    password_str = password_str[5:password_len]
    print('Actual password hashed value: ' + password_str)
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
                        "value": password_str
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
