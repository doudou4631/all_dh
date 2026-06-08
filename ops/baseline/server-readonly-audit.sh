#!/usr/bin/env bash

echo "== host/service =="
hostname
whoami
date
systemctl status geek-admin --no-pager | sed -n '1,40p'

echo "== frontend/mobile-h5 structure =="
ls -la /www/wwwroot/frontend
ls -la /www/wwwroot/frontend/mobile-h5
sed -n '1,120p' /www/wwwroot/frontend/mobile-h5/index.html
sed -n '1,160p' /www/wwwroot/frontend/mobile-h5/result/index.html

echo "== mysql readonly audit =="
sudo mysql -N -D verifynum < ops/baseline/server-db-audit.sql
