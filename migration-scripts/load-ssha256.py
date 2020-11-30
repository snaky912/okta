from csv import DictReader
import json
import requests
import base64

# This script servces as an example to import hashed passwords into
# Universal Directory.  As a sample, all user passwords are "abcd1234"

# Edit these variables based on the hash and org config
api_token = '00TeqPvqpgI6u7KX13h915elrgbKo0xGAYo24mszl1'
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

# Function to strip comments from file
def decomment(csv_file):
    for row in csv_file:
        raw = row.split('#')[0].strip()
        if raw: yield raw

# open file in read mode
with open('users.csv', 'r') as csv_file:
    csv_dict_reader = DictReader(decomment(csv_file))

    # iterate over each line
    for row in csv_dict_reader:
        password_str = row['hash']
        password_base64_decoded = base64.b64decode(password_str)
        # Password length of a SSHA256 hash is always 32 bytes long
        # Subtract 32 from the hash will give the length of the salt
        salt = password_base64_decoded[20:len(password_base64_decoded)]
        print('salt:' + salt.hex())
        print('salt base64 encoded: ' + base64.b64encode(salt).decode("utf-8"))
        password_hash = password_base64_decoded[0:20]
        print('password hash:' + password_hash.hex())
        print('password base64 encoded: ' + str(base64.b64encode(password_hash)))
        # populate the JSON payload with the user profile
        payload = {
                    "profile": {
                        "firstName": row['firstName'],
                        "lastName": row['lastName'],
                        "email": row['email'],
                        "login": row['email'],
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

        print(json.dumps(payload,indent=3))
        r = requests.post(tenant + url, headers=request_headers ,json=payload)
        if (r.status_code == 200):
            print('User ' + row['email'] + ' created.')
        else:
            json_response = r.json()
            print('Error: ' + str(r.status_code) + ' "' + json_response['errorSummary'] + '" Cause: ' + str(json_response['errorCauses']))
            #For debugging, print out the complete json response.
            #print(json.dumps(json_response,indent=2))
