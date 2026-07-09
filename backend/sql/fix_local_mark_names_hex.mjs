import { writeFileSync } from 'node:fs'
import { execSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))

const platforms = [
  ['mobile_gaopin', '\u79fb\u52a8\u9ad8\u9891'],
  ['td_gaopin', '\u6cf0\u8fea\u9ad8\u9891'],
  ['td_second', '\u6cf0\u8fea\u4e8c\u6b21'],
  ['qihu_first', '360\u9996\u6b21'],
  ['qihu_second', '360\u4e8c\u6b21'],
  ['dianhuabang', '\u7535\u8bdd\u90a6'],
  ['tencent_mark', '\u817e\u8baf'],
]

const toHex = (text) => Buffer.from(text, 'utf8').toString('hex').toUpperCase()

const templateInfo = JSON.stringify(
  platforms.map(([platformCode, platformName]) => ({
    platformCode,
    platformName,
    unitPrice: 1,
  }))
)

let sql = 'SET NAMES utf8mb4;\n'
sql += `UPDATE mark_platform_template SET template_name=CONVERT(UNHEX('${toHex('\u672c\u5730\u6d4b\u8bd5\u6a21\u677f')}') USING utf8mb4), template_info=CONVERT(UNHEX('${toHex(templateInfo)}') USING utf8mb4), update_by='admin', update_time=NOW() WHERE id=1;\n`

for (const [code, name] of platforms) {
  sql += `UPDATE mark_user_platform_quota SET platform_name=CONVERT(UNHEX('${toHex(name)}') USING utf8mb4) WHERE user_id=100001 AND platform_code='${code}';\n`
}

sql += `UPDATE sys_user SET nick_name=CONVERT(UNHEX('${toHex('\u6807\u8bb0\u7528\u6237')}') USING utf8mb4) WHERE user_name='markuser';\n`
sql += `UPDATE sys_user SET nick_name=CONVERT(UNHEX('${toHex('\u6807\u8bb0\u4ee3\u7406')}') USING utf8mb4) WHERE user_name='markagent';\n`

const sqlPath = join(__dirname, 'fix_local_mark_names_hex.sql')
writeFileSync(sqlPath, sql, 'ascii')

const mysql = 'C:/xampp/mysql/bin/mysql.exe'
execSync(
  `"${mysql}" -uverifyNum -ppL6NspjfTHLazatP --host=127.0.0.1 --default-character-set=utf8mb4 verifynum < "${sqlPath}"`,
  { shell: true, stdio: 'inherit' }
)

const out = execSync(
  `"${mysql}" -uverifyNum -ppL6NspjfTHLazatP --host=127.0.0.1 --default-character-set=utf8mb4 verifynum -N -B -e "SELECT HEX(platform_name) FROM mark_user_platform_quota WHERE user_id=100001 AND platform_code='mobile_gaopin'; SELECT HEX(template_info) FROM mark_platform_template WHERE id=1;"`,
  { shell: true, encoding: 'utf8' }
)

console.log(out)
console.log('expected mobile hex:', toHex('\u79fb\u52a8\u9ad8\u9891'))
