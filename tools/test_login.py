# -*- coding: utf-8 -*-
import json
import re
import urllib.request

BASE = 'http://127.0.0.1:8080'


def post(path, data):
    req = urllib.request.Request(
        BASE + path,
        data=json.dumps(data).encode('utf-8'),
        headers={'Content-Type': 'application/json'},
        method='POST',
    )
    with urllib.request.urlopen(req, timeout=10) as resp:
        return json.loads(resp.read().decode('utf-8'))


cap = post('/captcha/get', {'captchaType': 'math'})
cap_data = cap['data']
secret = cap_data.get('secretKey', '')
token = cap_data.get('token', '')

# math captcha: answer digits split into wordList
answer = '15'
word_list = list(answer)

login_body = {
    'username': '123456789',
    'password': '123456',
    'captchaVerification': '',
    'captchaType': 'math',
    'token': token,
    'secretKey': secret,
    'wordList': word_list,
}
try:
    result = post('/login', login_body)
    print(json.dumps(result, ensure_ascii=False, indent=2))
except urllib.error.HTTPError as e:
    print('HTTP', e.code, e.read().decode('utf-8', errors='replace'))
