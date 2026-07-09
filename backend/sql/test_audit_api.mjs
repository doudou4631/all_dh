import { execSync } from 'node:child_process'

const loginBody = JSON.stringify({ username: 'markagent', password: 'admin123' })
const loginResp = execSync(
  'curl.exe -s -X POST http://localhost:8080/login -H "Content-Type: application/json" -d @-',
  { input: loginBody, encoding: 'utf8' }
)
const loginJson = JSON.parse(loginResp)
const token = loginJson?.token
if (!token) {
  console.log('login failed:', loginResp)
  process.exit(1)
}
const pending = execSync(
  `curl.exe -s "http://localhost:8080/server/markAgent/audit/pending?pageNum=1&pageSize=5" -H "Authorization: Bearer ${token}"`,
  { encoding: 'utf8' }
)
console.log('audit/pending:', pending.slice(0, 500))
