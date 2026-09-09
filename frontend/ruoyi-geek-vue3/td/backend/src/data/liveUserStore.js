import { createHash } from 'node:crypto'
import { query } from './liveDb.js'

function md5(value = '') {
  return createHash('md5').update(String(value)).digest('hex')
}

function toStatusLabel(value = 0) {
  return Number(value || 0) === 1 ? '启用' : '禁用'
}

function toStatusValue(input = '') {
  if (input && typeof input === 'object') {
    if (Object.prototype.hasOwnProperty.call(input, 'value')) {
      const next = Number(input.value)
      if (next === 0 || next === 1) return next
    }
    if (Object.prototype.hasOwnProperty.call(input, 'label')) {
      return toStatusValue(input.label)
    }
  }

  if (typeof input === 'number') {
    return Number(input) === 0 ? 0 : 1
  }

  if (typeof input === 'boolean') {
    return input ? 1 : 0
  }

  const text = String(input ?? '').trim()
  if (!text) return 1
  if (text === '禁用' || text === '0') return 0
  if (/^(false|disable|disabled|off)$/i.test(text)) return 0
  return 1
}

function allow(value) {
  return value ? '允许' : '禁止'
}

async function getRoleMap() {
  const rows = await query('SELECT Id, RoleName FROM t_role ORDER BY Id')
  return rows.map((row) => ({ id: Number(row.Id), name: row.RoleName || '' }))
}

async function resolveRoleId(roleInput = '') {
  const roles = await getRoleMap()
  const text = String(roleInput || '').trim()
  if (!text) return Number(roles[0]?.id || 0)
  if (/^\d+$/.test(text)) return Number(text)
  const match = roles.find((item) => item.name === text)
  return Number(match?.id || roles[0]?.id || 0)
}

export async function findLiveUserByUsername(username = '') {
  const rows = await query(
    `SELECT u.Id, u.UserName, u.RealName, u.PassWord, u.RoleId, u.Status, u.daililevel, r.RoleName
       FROM t_user u
       LEFT JOIN t_role r ON r.Id = u.RoleId
      WHERE u.UserName = ?
      LIMIT 1`,
    [String(username).trim()],
  )
  return rows[0] || null
}

export async function findLiveUserById(userId = 0) {
      const rows = await query(
    `SELECT u.Id, u.UserName, u.RealName, u.PassWord, u.RoleId, u.Status, u.Phone, u.Email, u.Remark, u.Gender, u.HeadShot, u.bal,
            u.pricebiaoji, u.dealxiaomi, u.dealsougou, u.dealtdxgptput, u.dealtxthreeh, u.dealtx23d, u.dealltgj,
            u.dealydgaopin, u.dealbaidu, u.dealdianhb, u.dealup360, u.deal360first, u.dealyuyintdx, u.dealbaidunetdisk, u.dealsmsgettdx,
            u.CreateOn, u.UpdateOn, u.CreateBy, u.UpdateBy, u.parentid, u.daililevel, u.gonghuobiaoji, u.gonghuophoneguize, u.gonghuoemail, u.gonghuoqiyong, r.RoleName
       FROM t_user u
       LEFT JOIN t_role r ON r.Id = u.RoleId
      WHERE u.Id = ?
      LIMIT 1`,
    [Number(userId)],
  )
  return rows[0] || null
}

export async function listLiveUsers(keyword = '', roleName = '', status = '', options = {}) {
  const clauses = []
  const params = []

  const text = String(keyword || '').trim()
  if (text) {
    clauses.push('(u.UserName LIKE ? OR u.RealName LIKE ?)')
    params.push(`%${text}%`, `%${text}%`)
  }

  const role = String(roleName || '').trim()
  if (role) {
    clauses.push('r.RoleName = ?')
    params.push(role)
  }

  const normalizedStatus = String(status || '').trim()
  if (normalizedStatus === '启用' || normalizedStatus === '禁用') {
    clauses.push('u.Status = ?')
    params.push(toStatusValue(normalizedStatus))
  }

  const level = Number(options.level || 0)
  if (level > 0) {
    clauses.push('u.daililevel = ?')
    params.push(level)
  }

  const currentUserId = Number(options.currentUserId || 0)
  const applyAgentScope = Boolean(options.applyAgentScope && currentUserId > 0 && level > 0)
  if (applyAgentScope) {
    if (level === 1) {
      clauses.push('u.Id = ?')
      params.push(currentUserId)
    } else if (level === 2) {
      clauses.push('u.parentid = ?')
      params.push(currentUserId)
    } else if (level === 3) {
      const currentUser = await findLiveUserById(currentUserId)
      if (Number(currentUser?.daililevel || 0) === 2) {
        clauses.push('u.parentid = ?')
        params.push(currentUserId)
      } else {
        clauses.push('u.parentid IN (SELECT Id FROM t_user WHERE daililevel = 2 AND parentid = ?)')
        params.push(currentUserId)
      }
    }
  }

  const gonghuobiaoji = String(options.gonghuobiaoji || '').trim()
  if (gonghuobiaoji) {
    clauses.push('u.gonghuobiaoji = ?')
    params.push(gonghuobiaoji)
  }

  const where = clauses.length ? `WHERE ${clauses.join(' AND ')}` : ''
  const rows = await query(
    `SELECT u.Id, u.UserName, u.RealName, u.RoleId, u.Status, u.Phone, u.Email, u.Remark, u.Gender, u.HeadShot, u.bal, u.CreateOn, u.UpdateOn, u.CreateBy, u.UpdateBy, u.parentid, u.daililevel, u.gonghuobiaoji, u.gonghuophoneguize, u.gonghuoemail, u.gonghuoqiyong, r.RoleName
       FROM t_user u
       LEFT JOIN t_role r ON r.Id = u.RoleId
       ${where}
      ORDER BY u.Id DESC`,
    params,
  )
  return rows.map(mapLiveUserRow)
}

export async function validateLiveCredentials(username = '', password = '') {
  const user = await findLiveUserByUsername(username)
  if (!user || Number(user.Status || 0) !== 1) {
    return null
  }
  if (String(user.PassWord || '').toLowerCase() !== md5(password).toLowerCase()) {
    return null
  }
  return {
    id: `live-user-${user.Id}`,
    liveUserId: Number(user.Id),
    username: user.UserName,
    name: user.RealName || user.UserName,
    role: user.RoleName || '未分配角色',
    roleId: Number(user.RoleId || 0),
    daililevel: Number(user.daililevel || 0),
    status: '启用',
  }
}

export async function changeLiveUserPassword(userId, oldPassword, newPassword) {
  const rows = await query('SELECT Id, PassWord FROM t_user WHERE Id = ? LIMIT 1', [Number(userId)])
  const user = rows[0]
  if (!user) {
    return { ok: false, message: '未找到当前用户。' }
  }
  if (String(user.PassWord || '').toLowerCase() !== md5(oldPassword).toLowerCase()) {
    return { ok: false, message: '原密码不正确。' }
  }
  await query('UPDATE t_user SET PassWord = ?, UpdateOn = NOW() WHERE Id = ?', [md5(newPassword), Number(userId)])
  return { ok: true, message: '密码修改成功。' }
}

export async function resetLiveUserPassword(userId, nextPassword = '123456') {
  const user = await findLiveUserById(userId)
  if (!user) {
    return null
  }
  await query('UPDATE t_user SET PassWord = ?, UpdateOn = NOW() WHERE Id = ?', [md5(nextPassword), Number(userId)])
  return {
    id: `live-user-${user.Id}`,
    liveUserId: Number(user.Id),
    username: user.UserName,
    password: nextPassword,
  }
}

export async function deleteLiveUser(userId = 0) {
  const user = await findLiveUserById(userId)
  if (!user) return false
  await query('DELETE FROM t_user WHERE Id = ?', [Number(userId)])
  return true
}

export async function createLiveUser(payload = {}) {
  const username = String(payload.username || payload.UserName || '').trim()
  const realName = String(payload.name || payload.RealName || '').trim()
  const remark = String(payload.remark || payload.Remark || '').trim()
  const status = toStatusValue(payload.status ?? payload.Status ?? '启用')
  const password = String(payload.password || payload.PassWord || '123456')
  const roleId = await resolveRoleId(payload.roleId || payload.RoleId || payload.role)
  const parentId = Number(payload.parentId || payload.ParentId || 0)

  if (!username || !realName) {
    throw new Error('用户名和真实姓名不能为空。')
  }
  const exists = await findLiveUserByUsername(username)
  if (exists) {
    throw new Error('用户名已存在。')
  }

  let daililevel = Number(payload.daililevel || payload.DaiLiLevel || 0)
  if (!daililevel && parentId > 0) {
    const parent = await findLiveUserById(parentId)
    daililevel = Math.max(1, Number(parent?.daililevel || 0) + 1)
  }
  if (!daililevel) {
    const roleText = String(payload.role || '').trim()
    if (roleText.includes('一级')) daililevel = 1
    else if (roleText.includes('二级')) daililevel = 2
    else if (roleText.includes('三级')) daililevel = 3
  }

  await query(
    `INSERT INTO t_user (UserName, RealName, PassWord, Gender, Phone, Email, treepath, Remark, HeadShot, RoleId, Status, CreateOn, UpdateOn, CreateBy, UpdateBy, bal, parentid, daililevel)
     VALUES (?, ?, ?, 0, '', '', '', ?, '', ?, ?, NOW(), NOW(), 0, 0, 0, ?, ?)`,
    [username, realName, md5(password), remark, roleId, status, parentId, daililevel],
  )
  const inserted = await findLiveUserByUsername(username)
  return inserted ? mapLiveUserRow(inserted) : null
}

export async function updateLiveUser(userId, payload = {}) {
  const current = await findLiveUserById(userId)
  if (!current) return null
  const username = String(payload.username ?? payload.UserName ?? current.UserName).trim()
  const realName = String(payload.name ?? payload.RealName ?? current.RealName).trim()
  const remark = String(payload.remark ?? payload.Remark ?? current.Remark ?? '').trim()
  const status = toStatusValue(payload.status ?? payload.Status ?? toStatusLabel(current.Status))
  const roleId = await resolveRoleId(payload.roleId || payload.RoleId || payload.role || current.RoleId)

  const duplicate = await findLiveUserByUsername(username)
  if (duplicate && Number(duplicate.Id) !== Number(userId)) {
    throw new Error('用户名已存在。')
  }

  await query(
    'UPDATE t_user SET UserName = ?, RealName = ?, Remark = ?, RoleId = ?, Status = ?, UpdateOn = NOW() WHERE Id = ?',
    [username, realName, remark, roleId, status, Number(userId)],
  )
  const updated = await findLiveUserById(userId)
  return updated ? mapLiveUserRow(updated) : null
}

function hasAny(actions = [], targets = []) {
  return targets.some((item) => actions.includes(item))
}

function mapRoleRows(roleRows = [], menus = []) {
  const grouped = new Map()
  for (const row of roleRows) {
    const menuId = Number(row.MenuId)
    const actions = grouped.get(menuId) || []
    actions.push(Number(row.ActionId))
    grouped.set(menuId, actions)
  }

  const menuMap = new Map(menus.map(m => [Number(m.Id), m]))

  function getRootParent(menuId) {
    let current = menuMap.get(Number(menuId))
    if (!current) return null
    let depth = 0
    while (current.ParentId !== 0 && depth < 10) {
      const parent = menuMap.get(Number(current.ParentId))
      if (!parent) break
      current = parent
      depth++
    }
    return current
  }

  const modulePerms = {
    '权限管理': { read: false, create: false, edit: false, delete: false },
    '用户管理': { read: false, create: false, edit: false, delete: false },
    '代理管理': { read: false, create: false, edit: false, delete: false },
    '系统设置': { read: false, create: false, edit: false, delete: false },
    '网站设置': { read: false, create: false, edit: false, delete: false },
    '基本资料': { read: false, create: false, edit: false, delete: false },
    '修改密码': { read: false, create: false, edit: false, delete: false },
    '日志管理': { read: false, create: false, edit: false, delete: false },
    '审计日志': { read: false, create: false, edit: false, delete: false },
    '财务流水': { read: false, create: false, edit: false, delete: false },
    '泰迪熊标记': { read: false, create: false, edit: false, delete: false },
    '标记处理': { read: false, create: false, edit: false, delete: false },
    '供货管理': { read: false, create: false, edit: false, delete: false }
  }

  const permissionActionIds = {
    read: [0, 3],
    create: [1],
    edit: [2, 6, 8],
    delete: [4, 7]
  }

  for (const [menuId, actions] of grouped.entries()) {
    const menu = menuMap.get(menuId)
    if (!menu) continue

    const root = getRootParent(menuId)
    const rootName = root ? String(root.MenuName || '') : ''
    const menuName = String(menu.MenuName || '')
    const menuUrl = String(menu.MenuUrl || '').toLowerCase()

    let moduleName = ''

    if (menuName.includes('用户') || menuId === 75) {
      moduleName = '用户管理'
    } else if (rootName === '权限管理' || menuUrl.includes('/permissions/')) {
      moduleName = '权限管理'
    } else if (menuName.includes('泰迪') || menuName.includes('tdx') || menuName.includes('TDX') || menuName.includes('TD(') || menuId === 67 || menuId === 85 || menuId === 99 || menuId === 100 || menuId === 101) {
      moduleName = '泰迪熊标记'
    } else if (menuName.includes('利润') || menuName.includes('财务') || menuName.includes('资金') || menuId === 108) {
      moduleName = '财务流水'
    } else if (rootName === '供货管理' || menuName.includes('供货') || menuId === 107) {
      moduleName = '供货管理'
    } else if (rootName === '代理管理' || menuName.includes('代理') || menuId === 106) {
      moduleName = '代理管理'
    } else if (menuName.includes('网站设置') || menuId === 7) {
      moduleName = '网站设置'
    } else if (menuName.includes('基本资料') || menuId === 8) {
      moduleName = '基本资料'
    } else if (menuName.includes('修改密码') || menuId === 9) {
      moduleName = '修改密码'
    } else if (menuName.includes('日志') || menuId === 10) {
      moduleName = '日志管理'
      const targetAudit = modulePerms['审计日志']
      targetAudit.read = targetAudit.read || hasAny(actions, permissionActionIds.read)
      targetAudit.delete = targetAudit.delete || hasAny(actions, permissionActionIds.delete)
    } else if (menuId === 6) {
      moduleName = '系统设置'
    } else if (rootName === '标记处理' || menuUrl.includes('/orderdeal') || menuUrl.includes('/order/') || menuId === 116) {
      moduleName = '标记处理'
    } else {
      const normalizedRoot = rootName.trim()
      if (modulePerms[normalizedRoot]) {
        moduleName = normalizedRoot
      }
    }

    if (moduleName && modulePerms[moduleName]) {
      const target = modulePerms[moduleName]
      target.read = target.read || hasAny(actions, permissionActionIds.read)
      target.create = target.create || hasAny(actions, permissionActionIds.create)
      target.edit = target.edit || hasAny(actions, permissionActionIds.edit)
      target.delete = target.delete || hasAny(actions, permissionActionIds.delete)
    }
  }

  return Object.entries(modulePerms).map(([name, perms]) => ({
    module: name,
    read: allow(perms.read),
    create: allow(perms.create),
    edit: allow(perms.edit),
    delete: allow(perms.delete)
  }))
}

export async function getLiveAccessModules(roleId = 0) {
  const roleRows = await query('SELECT MenuId, ActionId FROM t_menu_role_action WHERE RoleId = ?', [Number(roleId)])
  const menus = await query('SELECT Id, MenuName, MenuUrl, ParentId FROM t_menu WHERE Status = 1')
  return mapRoleRows(roleRows, menus)
}

export function mapLiveUserRow(row) {
  return {
    id: `live-user-${row.Id}`,
    liveUserId: Number(row.Id),
    username: row.UserName,
    name: row.RealName || row.UserName,
    role: row.RoleName || '',
    roleId: Number(row.RoleId || 0),
    phone: row.Phone || '',
    email: row.Email || '',
    remark: row.Remark || '',
    balance: String(row.bal ?? 0),
    parentId: Number(row.parentid || 0),
    daililevel: Number(row.daililevel || 0),
    gonghuobiaoji: String(row.gonghuobiaoji ?? ''),
    gonghuophoneguize: String(row.gonghuophoneguize ?? ''),
    gonghuoemail: String(row.gonghuoemail ?? ''),
    gonghuoqiyong: String(row.gonghuoqiyong ?? ''),
    pricebiaoji: String(row.pricebiaoji ?? 0),
    dealxiaomi: String(row.dealxiaomi ?? 0),
    dealsougou: String(row.dealsougou ?? 0),
    dealtdxgptput: String(row.dealtdxgptput ?? 0),
    dealtxthreeh: String(row.dealtxthreeh ?? 0),
    dealtx23d: String(row.dealtx23d ?? 0),
    dealltgj: String(row.dealltgj ?? 0),
    dealydgaopin: String(row.dealydgaopin ?? 0),
    dealbaidu: String(row.dealbaidu ?? 0),
    dealdianhb: String(row.dealdianhb ?? 0),
    dealup360: String(row.dealup360 ?? 0),
    deal360first: String(row.deal360first ?? 0),
    dealyuyintdx: String(row.dealyuyintdx ?? 0),
    dealbaidunetdisk: String(row.dealbaidunetdisk ?? 0),
    dealsmsgettdx: String(row.dealsmsgettdx ?? 0),
    status: toStatusLabel(row.Status),
    updated: row.UpdateOn || row.CreateOn || '',
    createdOn: row.CreateOn || '',
    createBy: String(row.CreateBy ?? ''),
    updateBy: String(row.UpdateBy ?? ''),
  }
}

function parseMoney(value, fallback = 0) {
  const num = Number(String(value ?? '').trim())
  return Number.isFinite(num) ? num : fallback
}

function buildOrderNumber(prefix = 'SystemAddMoney') {
  const now = new Date()
  const pad = (value, size = 2) => String(value).padStart(size, '0')
  const stamp = `${pad(now.getFullYear() % 100)}${pad(now.getMonth() + 1)}${pad(now.getDate())}_${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}${pad(now.getMilliseconds(), 3)}`
  return `${prefix}_${stamp}_${md5(`${prefix}-${now.getTime()}-${Math.random()}`).slice(0, 16)}_`
}

async function createMoneyRecord({
  userId,
  username,
  beforeAmount,
  delta,
  afterAmount,
  type,
  orderNumber,
  orderType,
  phone = '',
  note = '',
  operatorId = 0,
}) {
  await query(
    `INSERT INTO t_money (userid, username, \`before\`, money, \`after\`, type, ordernumber, ordertype, phone, note, CreateOn, UpdateOn, CreateBy, UpdateBy, createtime)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), ?, ?, NOW())`,
    [Number(userId), username, beforeAmount, delta, afterAmount, type, orderNumber, orderType, phone, note, Number(operatorId), Number(operatorId)],
  )
}

async function createMoneyFirstRecord({
  fromUserId,
  fromUsername,
  toUserId,
  toUsername,
  amount,
  orderNumber,
  note,
  operatorId = 0,
}) {
  await query(
    `INSERT INTO t_moneyfirst (useridfrom, usernamefrom, money, ordernumber, useridto, usernameto, note, createtime, CreateOn, UpdateOn, CreateBy, UpdateBy)
     VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), NOW(), ?, ?)`,
    [Number(fromUserId), fromUsername, amount, orderNumber, Number(toUserId), toUsername, note, Number(operatorId), Number(operatorId)],
  )
}

export async function saveLiveUserMoneyAction(userId = 0, payload = {}, mode = 'mine_addmoney', operator = {}) {
  const current = await findLiveUserById(userId)
  if (!current) return null
  const operatorId = Number(operator.liveUserId || operator.id || 0)
  const operatorUsername = String(operator.username || '')
  if (mode === 'mine_setmoney') {
    const fields = {
      pricebiaoji: String(payload.pricebiaoji ?? current.pricebiaoji ?? '0'),
      dealxiaomi: String(payload.dealxiaomi ?? current.dealxiaomi ?? '0'),
      dealsougou: String(payload.dealsougou ?? current.dealsougou ?? '0'),
      dealyuyintdx: String(payload.dealyuyintdx ?? current.dealyuyintdx ?? '0'),
      dealtdxgptput: String(payload.dealtdxgptput ?? current.dealtdxgptput ?? '0'),
      dealtxthreeh: String(payload.dealtxthreeh ?? current.dealtxthreeh ?? '0'),
      dealtx23d: String(payload.dealtx23d ?? current.dealtx23d ?? '0'),
      dealltgj: String(payload.dealltgj ?? current.dealltgj ?? '0'),
      dealup360: String(payload.dealup360 ?? current.dealup360 ?? '0'),
      dealbaidu: String(payload.dealbaidu ?? current.dealbaidu ?? '0'),
      dealdianhb: String(payload.dealdianhb ?? current.dealdianhb ?? '0'),
      deal360first: String(payload.deal360first ?? current.deal360first ?? '0'),
      dealydgaopin: String(payload.dealydgaopin ?? current.dealydgaopin ?? '0'),
      dealbaidunetdisk: String(payload.dealbaidunetdisk ?? current.dealbaidunetdisk ?? '0'),
      dealsmsgettdx: String(payload.dealsmsgettdx ?? current.dealsmsgettdx ?? '0'),
      Remark: String(payload.note ?? payload.remark ?? current.Remark ?? ''),
    }
    await query(
      `UPDATE t_user SET pricebiaoji=?, dealxiaomi=?, dealsougou=?, dealyuyintdx=?, dealtdxgptput=?, dealtxthreeh=?, dealtx23d=?, dealltgj=?, dealup360=?, dealbaidu=?, dealdianhb=?, deal360first=?, dealydgaopin=?, dealbaidunetdisk=?, dealsmsgettdx=?, Remark=?, UpdateOn=NOW() WHERE Id=?`,
      [
        fields.pricebiaoji, fields.dealxiaomi, fields.dealsougou, fields.dealyuyintdx, fields.dealtdxgptput,
        fields.dealtxthreeh, fields.dealtx23d, fields.dealltgj, fields.dealup360, fields.dealbaidu,
        fields.dealdianhb, fields.deal360first, fields.dealydgaopin, fields.dealbaidunetdisk, fields.dealsmsgettdx,
        fields.Remark, Number(userId),
      ],
    )
  } else if (mode === 'mine_wxgonghuoset') {
    await query(
      `UPDATE t_user SET gonghuobiaoji=?, gonghuophoneguize=?, gonghuoemail=?, gonghuoqiyong=?, Remark=?, UpdateOn=NOW() WHERE Id=?`,
      [
        String(payload.gonghuobiaoji ?? current.gonghuobiaoji ?? ''),
        String(payload.gonghuophoneguize ?? current.gonghuophoneguize ?? ''),
        String(payload.gonghuoemail ?? current.gonghuoemail ?? ''),
        String(payload.gonghuoqiyong ?? current.gonghuoqiyong ?? ''),
        String(payload.note ?? payload.remark ?? current.Remark ?? ''),
        Number(userId),
      ],
    )
  } else if (mode === 'erji_addmoney') {
    const amount = parseMoney(payload.money, 0)
    if (amount <= 0) {
      throw new Error('加款金额必须大于 0。')
    }
    const parentId = Number(current.parentid || 0)
    if (parentId <= 0) {
      throw new Error('当前账号没有上级代理，无法执行对子账户加款。')
    }
    const parent = await findLiveUserById(parentId)
    if (!parent) {
      throw new Error('未找到上级代理账号。')
    }
    const parentBalance = parseMoney(parent.bal, 0)
    if (parentBalance < amount) {
      throw new Error('上级代理余额不足，无法完成转账。')
    }
    const childBalance = parseMoney(current.bal, 0)
    await query('UPDATE t_user SET bal = ?, UpdateOn = NOW() WHERE Id = ?', [parentBalance - amount, parentId])
    await query('UPDATE t_user SET bal = ?, Remark = ?, UpdateOn = NOW() WHERE Id = ?', [
      childBalance + amount,
      String(payload.note ?? payload.remark ?? current.Remark ?? ''),
      Number(userId),
    ])
    const orderNumber = buildOrderNumber('YiJiDaiLi_AddMoney')
    const note = `[上级]=${parentId}#${parent.UserName || ''}给[下级]=${current.Id}#${current.UserName || ''}[转账=]【${amount}】详情:关于YiJiDaiLi_AddMoney[ordernumber=]${orderNumber}`
    await createMoneyRecord({
      userId: parentId,
      username: parent.UserName || '',
      beforeAmount: parentBalance,
      delta: amount,
      afterAmount: parentBalance - amount,
      type: 5,
      orderNumber,
      orderType: 'YiJiDaiLi_AddMoney',
      phone: 'YiJiDaiLi_AddMoney',
      note,
      operatorId,
    })
    await createMoneyRecord({
      userId: current.Id,
      username: current.UserName || '',
      beforeAmount: childBalance,
      delta: amount,
      afterAmount: childBalance + amount,
      type: 5,
      orderNumber,
      orderType: 'YiJiDaiLi_AddMoney',
      phone: 'YiJiDaiLi_AddMoney',
      note,
      operatorId,
    })
    await createMoneyFirstRecord({
      fromUserId: parentId,
      fromUsername: parent.UserName || '',
      toUserId: current.Id,
      toUsername: current.UserName || '',
      amount,
      orderNumber,
      note,
      operatorId,
    })
  } else {
    const amount = parseMoney(payload.money, 0)
    if (amount <= 0) {
      throw new Error('加款金额必须大于 0。')
    }
    const orderNumber = buildOrderNumber('SystemAddMoney')
    const beforeAmount = parseMoney(current.bal, 0)
    const nextBalance = parseMoney(current.bal, 0) + amount
    await query('UPDATE t_user SET bal = ?, Remark = ?, UpdateOn = NOW() WHERE Id = ?', [
      nextBalance,
      String(payload.note ?? payload.remark ?? current.Remark ?? ''),
      Number(userId),
    ])
    await createMoneyRecord({
      userId: current.Id,
      username: current.UserName || '',
      beforeAmount,
      delta: amount,
      afterAmount: nextBalance,
      type: 2,
      orderNumber,
      orderType: 'SystemAddMoney',
      phone: 'SystemAddMoney',
      note: String(payload.note ?? payload.remark ?? `关于SystemAddMoney的动作_${operatorUsername || current.UserName || ''}`),
      operatorId,
    })
  }
  const updated = await findLiveUserById(userId)
  return updated ? mapLiveUserRow(updated) : null
}

function normalizePrice(value) {
  const text = String(value ?? '').trim()
  if (!text) return '0'
  return text.replace(/^-/, '')
}

function buildDashboardPanel(row) {
  const specialRole = [19, 20].includes(Number(row.RoleId || 0))
  if (specialRole) {
    return {
      mode: 'special',
      items: [
        { label: '账户名称', value: row.UserName || '' },
        { label: '当前剩余', value: String(row.bal ?? '0') },
        { label: 'tdx二次申诉', value: normalizePrice(row.dealsmsgettdx) + '/个' },
      ],
    }
  }

  return {
    mode: 'full',
    items: [
      { label: '账户名称', value: row.UserName || '' },
      { label: '当前余额', value: String(row.bal ?? '0') },
      { label: '查询标记', value: normalizePrice(row.pricebiaoji) },
      { label: '清除小米标记', value: normalizePrice(row.dealxiaomi) },
      { label: '清除搜狗标记', value: normalizePrice(row.dealsougou) },
      { label: '泰迪熊高频', value: normalizePrice(row.dealtdxgptput) },
      { label: '腾讯速清理(3小时+验证码)', value: normalizePrice(row.dealtxthreeh) },
      { label: '清腾讯【2-3天；无码】', value: normalizePrice(row.dealtx23d) },
      { label: '清联通(2-5天)', value: normalizePrice(row.dealltgj) },
      { label: '清移动高频(2-3天)', value: normalizePrice(row.dealydgaopin) },
      { label: '清百度标记(无码)', value: normalizePrice(row.dealbaidu) },
      { label: '清电话邦标记(无码)', value: normalizePrice(row.dealdianhb) },
      { label: '改360标识(三选一)', value: normalizePrice(row.dealup360) },
      { label: '清360标记(无码)', value: normalizePrice(row.deal360first) },
      { label: '泰迪熊发短信(二次)', value: normalizePrice(row.dealyuyintdx) },
      { label: 'tdx二次申诉', value: normalizePrice(row.dealsmsgettdx) },
    ],
  }
}

export async function getLiveProfile(userId = 0) {
  const row = await findLiveUserById(userId)
  if (!row) return null
  return {
    id: `live-user-${row.Id}`,
    liveUserId: Number(row.Id),
    username: row.UserName,
    realName: row.RealName || '',
    roleId: Number(row.RoleId || 0),
    roleName: row.RoleName || '',
    balance: String(row.bal ?? 0),
    dealtxthreeh: Number(row.dealtxthreeh || 0),
    dealtx23d: Number(row.dealtx23d || 0),
    gender: Number(row.Gender || 0),
    phone: row.Phone || '',
    email: row.Email || '',
    remark: row.Remark || '',
    headShot: row.HeadShot || '',
    dashboardPanel: buildDashboardPanel(row),
  }
}

export async function updateLiveProfile(userId = 0, payload = {}) {
  const current = await findLiveUserById(userId)
  if (!current) return null
  const realName = String(payload.realName ?? payload.RealName ?? current.RealName ?? '').trim()
  const phone = String(payload.phone ?? payload.Phone ?? current.Phone ?? '').trim()
  const email = String(payload.email ?? payload.Email ?? current.Email ?? '').trim()
  const remark = String(payload.remark ?? payload.Remark ?? current.Remark ?? '').trim()
  const headShot = String(payload.headShot ?? payload.HeadShot ?? current.HeadShot ?? '').trim()
  const gender = Number(payload.gender ?? payload.Gender ?? current.Gender ?? 0)

  await query(
    'UPDATE t_user SET RealName = ?, Phone = ?, Email = ?, Remark = ?, HeadShot = ?, Gender = ?, UpdateOn = NOW() WHERE Id = ?',
    [realName, phone, email, remark, headShot, gender, Number(userId)],
  )

  return getLiveProfile(userId)
}
