#!/bin/bash
set -e

# Script to generate a release signing key for Sovereign Android App
KEYSTORE_NAME="my-upload-key.jks"
KEY_ALIAS="upload"
VALIDITY_DAYS=10000

echo "=== Sovereign Release Keystore Generator ==="
echo ""

if [ -f "$KEYSTORE_NAME" ]; then
    echo "Warning: $KEYSTORE_NAME already exists in the current directory."
    read -p "Overwrite? (y/N): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 1
    fi
    rm -f "$KEYSTORE_NAME"
fi

read -s -p "Enter password for keystore and key: " PASSWORD
echo ""
read -s -p "Confirm password: " PASSWORD_CONFIRM
echo ""

if [ "$PASSWORD" != "$PASSWORD_CONFIRM" ]; then
    echo "Error: Passwords do not match!"
    exit 1
fi

if [ -z "$PASSWORD" ]; then
    echo "Error: Password cannot be empty!"
    exit 1
fi

echo "Generating keystore with RSA 2048-bit key..."

keytool -genkey -v \
    -keystore "$KEYSTORE_NAME" \
    -storepass "$PASSWORD" \
    -alias "$KEY_ALIAS" \
    -keypass "$PASSWORD" \
    -keyalg RSA \
    -keysize 2048 \
    -validity "$VALIDITY_DAYS" \
    -dname "CN=Sovereign Release,OU=Mobile,O=Sovereign,L=London,C=GB"

echo ""
echo "=== Keystore Generated Successfully: $KEYSTORE_NAME ==="
echo ""
echo "--- GitHub Actions Configuration Guide ---"
echo "To sign release builds automatically in GitHub Actions:"
echo "1. Go to your GitHub repository -> Settings -> Secrets and variables -> Actions"
echo "2. Add the following repository secrets:"
echo "   - KEYSTORE_BASE64: Copy the base64 string below"
echo "   - STORE_PASSWORD: The password you just entered"
echo "   - KEY_PASSWORD:   The password you just entered"
echo ""
echo "Base64 encoded keystore string (copy between lines):"
echo "--------------------------------------------------------"
if command -v base64 >/dev/null 2>&1; then
    base64 -w 0 "$KEYSTORE_NAME" || base64 "$KEYSTORE_NAME"
fi
echo ""
echo "--------------------------------------------------------"
echo "Keep $KEYSTORE_NAME in a secure location and DO NOT commit it directly to public repositories."
