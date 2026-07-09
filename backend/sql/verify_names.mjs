import { execSync } from 'node:child_process'

const mysql =
  'C:/xampp/mysql/bin/mysql.exe -uverifyNum -ppL6NspjfTHLazatP --host=127.0.0.1 --default-character-set=utf8mb4 verifynum'

function query(sql) {
  return execSync(`${mysql} -N -B -e "${sql}"`, { encoding: 'utf8' }).trim()
}

const hex = query("SELECT HEX(platform_name) FROM mark_user_platform_quota WHERE user_id=100001 AND platform_code='mobile_gaopin'")
const name = Buffer.from(hex, 'hex').toString('utf8')
console.log('db name:', JSON.stringify(name))
console.log('expected:', JSON.stringify('\u79fb\u52a8\u9ad8\u9891'))
console.log('match:', name === '\u79fb\u52a8\u9ad8\u9891')

const templateHex = query('SELECT HEX(template_info) FROM mark_platform_template WHERE id=1')
const templateInfo = Buffer.from(templateHex, 'hex').toString('utf8')
console.log('template first platform:', JSON.parse(templateInfo)[0])
