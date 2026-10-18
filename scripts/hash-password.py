#!/usr/bin/env python3

import base64
import hashlib
import secrets
import sys


password = sys.stdin.buffer.read()
salt = secrets.token_bytes(16)
derived_key = hashlib.pbkdf2_hmac("sha256", password, salt, 310_000, dklen=32)
print(
    "pbkdf2-sha256$310000$"
    + base64.b64encode(salt).decode("ascii")
    + "$"
    + base64.b64encode(derived_key).decode("ascii")
)
