import { writeFileSync } from 'node:fs'
import { execSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))

const platforms = [
  ['mobile_gaopin', '??????'],
  ['td_gaopin', '?????'],
  ['td_second', '??????'],
  ['qihu_first', '360???'],
  ['qihu_second', '360????'],
  ['dianhuabang', '?????'],
  ['tencent_mark', '???'],
]

const esc = (value) => String(value).replace(/\\/g, '\\\\').replace(/'/g, "''")

const templateInfo = JSON.stringify(
  platforms.map(([platformCode, platformName]) => ({
    platformCode,
    platformName,
    unitPrice: 1,
  }))
)

let sql = 'SET NAMES utf8mb4;\n'
sql += `UPDATE mark_platform_template SET template_name='${esc('??????????')}', template_info='${esc(templateInfo)}', update_by='admin', update_time=NOW() WHERE id=1;\n`

for (const [code, name] of platforms) {
  sql += `UPDATE mark_user_platform_quota SET platform_name='${esc(name)}' WHERE user_id=100001 AND platform_code='${esc(code)}';\n`
}

sql += "UPDATE sys_user SET nick_name='??????' WHERE user_name='markuser';\n"
sql += "UPDATE sys_user SET nick_name='??????' WHERE user_name='markagent';\n"

const sqlPath = join(__dirname, 'fix_local_mark_names_utf8.sql')
writeFileSync(sqlPath, sql, 'utf8')

const mysql = 'C:/xampp/mysql/bin/mysql.exe'
execSync(
  `"${mysql}" -uverifyNum -ppL6NspjfTHLazatP --host=127.0.0.1 --default-character-set=utf8mb4 verifynum < "${sqlPath}"`,
  { shell: true, stdio: 'inherit' }
)

const verify = execSync(
  `${mysql} -uverifyNum -ppL6NspjfTHLazatP --host=127.0.0.1 --default-character-set=utf8mb4 verifynum -e "SELECT template_name FROM mark_platform_template WHERE id=1; SELECT platform_code, platform_name FROM mark_user_platform_quota WHERE user_id=100001;"`,
  { encoding: 'utf8' }
)

console.log(verify)
