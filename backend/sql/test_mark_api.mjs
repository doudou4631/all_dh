import { execSync } from 'node:child_process'

const mysql =
  'C:/xampp/mysql/bin/mysql.exe -uverifyNum -ppL6NspjfTHLazatP --host=127.0.0.1 --default-character-set=utf8mb4 verifynum'

function query(sql) {
  return execSync(`${mysql} -N -B -e "${sql}"`, { encoding: 'utf8' }).trim()
}

const templateInfoHex = query('SELECT HEX(template_info) FROM mark_platform_template WHERE id=1')
const templateInfo = Buffer.from(templateInfoHex, 'hex').toString('utf8')
console.log('template_info:', templateInfo)

const platformName = query("SELECT platform_name FROM mark_user_platform_quota WHERE user_id=100001 AND platform_code='mobile_gaopin'")
console.log('quota name:', JSON.stringify(platformName))

try {
  const loginBody = JSON.stringify({ username: 'markuser', password: 'admin123' })
  const loginResp = execSync(
    'curl.exe -s -X POST http://localhost:8080/login -H "Content-Type: application/json" -d @-',
    { input: loginBody, encoding: 'utf8' }
  )
  const loginJson = JSON.parse(loginResp)
  const token = loginJson?.token
  if (!token) {
    console.log('login failed:', loginResp.slice(0, 500))
    process.exit(1)
  }
  const priceResp = execSync(
    'curl.exe -s http://localhost:8080/server/markUser/price/list -H "Authorization: Bearer PLACEHOLDER"',
    { encoding: 'utf8' }
  ).replace('PLACEHOLDER', token)
  console.log('price/list:', priceResp.slice(0, 800))
} catch (error) {
  console.error(String(error))
}
