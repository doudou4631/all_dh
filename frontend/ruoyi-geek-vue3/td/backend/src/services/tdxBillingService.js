import { query, withTransaction } from '../data/liveDb.js'

export const TDX_SMS_RECEIVE_COMPANY_CODE = 'duanxingettaidixiong'
export const TDX_SMS_RECEIVE_COMPANY_NAME = 'tdx二次申诉'
export const TDX_SMS_RECEIVE_INSUFFICIENT_BALANCE_MESSAGE = '余额不足，请先充值后再提交。'

const TDX_SMS_RECEIVE_ORDER_TYPE = 'BiaoJiDeal_DuanXinGetDealTaiDiX_'
const TDX_SMS_RECEIVE_CHARGE_TYPE = 3
const TDX_SMS_RECEIVE_REFUND_TYPE = 4

function normalizeText(value = '') {
  return String(value || '').trim()
}

function parseMoney(value, fallback = 0) {
  const num = Number(String(value ?? '').trim())
  return Number.isFinite(num) ? num : fallback
}

function toPositiveAmount(value = 0) {
  return Math.max(0, Math.abs(parseMoney(value, 0)))
}

function summarizeOutstandingCharges(moneyRows = []) {
  const chargeCount = moneyRows.filter((row) => Number(row.type || 0) === TDX_SMS_RECEIVE_CHARGE_TYPE && Number(row.money || 0) < 0).length
  const refundCount = moneyRows.filter((row) => Number(row.type || 0) === TDX_SMS_RECEIVE_REFUND_TYPE && Number(row.money || 0) > 0).length
  return {
    chargeCount,
    refundCount,
    outstandingCharges: chargeCount - refundCount,
  }
}

async function findLatestTdxSmsReceiveOrderDealByPhone(userId = 0, phone = '', createdAt = null) {
  const safeUserId = Number(userId || 0)
  const normalizedPhone = normalizeText(phone)
  if (!safeUserId || !normalizedPhone) return null
  const rows = await query(
    `SELECT Id, orderpicinumber, orderprice
       FROM t_orderdeal
      WHERE userid = ?
        AND phone = ?
        AND companycode = ?
        AND createtime >= DATE(COALESCE(?, NOW()))
        AND createtime < DATE_ADD(DATE(COALESCE(?, NOW())), INTERVAL 1 DAY)
      ORDER BY Id DESC
      LIMIT 1`,
    [safeUserId, normalizedPhone, TDX_SMS_RECEIVE_COMPANY_CODE, createdAt, createdAt],
  )
  return rows[0] || null
}

async function findTdxSmsReceiveOrderDealById(orderDealId = 0) {
  const safeOrderDealId = Number(orderDealId || 0)
  if (!safeOrderDealId) return null
  const rows = await query(
    'SELECT Id, orderpicinumber, orderprice FROM t_orderdeal WHERE Id = ? LIMIT 1',
    [safeOrderDealId],
  )
  return rows[0] || null
}

async function loadTdxSmsReceiveOutstandingCharge(userId = 0, orderNumber = '') {
  const safeUserId = Number(userId || 0)
  const safeOrderNumber = normalizeText(orderNumber)
  if (!safeUserId || !safeOrderNumber) {
    return { chargeCount: 0, refundCount: 0, outstandingCharges: 0 }
  }

  const moneyRows = await query(
    `SELECT Id, type, money
       FROM t_money
      WHERE userid = ?
        AND ordernumber = ?
        AND ordertype = ?
      ORDER BY Id ASC`,
    [safeUserId, safeOrderNumber, TDX_SMS_RECEIVE_ORDER_TYPE],
  )
  return summarizeOutstandingCharges(moneyRows)
}

export function isTdxSmsReceiveInsufficientBalanceResult(result = {}) {
  const reason = normalizeText(result?.reason)
  const action = normalizeText(result?.action)
  return reason === 'insufficient-balance' && (result?.ok === false || action === 'blocked')
}

export async function getTdxSmsReceiveBalanceGuard({
  userId = 0,
  phone = '',
  orderDealId = 0,
  orderPrice = null,
  createdAt = null,
} = {}) {
  const safeUserId = Number(userId || 0)
  if (!safeUserId) {
    return {
      ok: false,
      reason: 'missing-user-id',
      message: '当前账号不存在，无法校验余额。',
      userId: 0,
      username: '',
      balance: 0,
      price: 0,
      requiredAmount: 0,
      deficit: 0,
      needsCharge: false,
      orderDealId: 0,
      orderNumber: '',
      chargeCount: 0,
      refundCount: 0,
      outstandingCharges: 0,
    }
  }

  const userRows = await query(
    'SELECT Id, UserName, bal, dealsmsgettdx FROM t_user WHERE Id = ? LIMIT 1',
    [safeUserId],
  )
  const user = userRows[0]
  if (!user) {
    return {
      ok: false,
      reason: 'user-not-found',
      message: '当前账号不存在，无法校验余额。',
      userId: safeUserId,
      username: '',
      balance: 0,
      price: 0,
      requiredAmount: 0,
      deficit: 0,
      needsCharge: false,
      orderDealId: 0,
      orderNumber: '',
      chargeCount: 0,
      refundCount: 0,
      outstandingCharges: 0,
    }
  }

  const balance = parseMoney(user.bal, 0)
  const username = normalizeText(user.UserName)
  let resolvedOrderDealId = Number(orderDealId || 0)
  let order = resolvedOrderDealId ? await findTdxSmsReceiveOrderDealById(resolvedOrderDealId) : null
  if (!order && phone) {
    order = await findLatestTdxSmsReceiveOrderDealByPhone(safeUserId, phone, createdAt)
    resolvedOrderDealId = Number(order?.Id || 0)
  }

  const priceFromUser = toPositiveAmount(user.dealsmsgettdx)
  const resolvedPrice = toPositiveAmount(order?.orderprice ?? orderPrice ?? priceFromUser) || priceFromUser
  const orderNumber = normalizeText(order?.orderpicinumber)
  const outstanding = orderNumber
    ? await loadTdxSmsReceiveOutstandingCharge(safeUserId, orderNumber)
    : { chargeCount: 0, refundCount: 0, outstandingCharges: 0 }
  const needsCharge = resolvedPrice > 0 && outstanding.outstandingCharges <= 0
  const requiredAmount = needsCharge ? resolvedPrice : 0
  const deficit = Math.max(0, requiredAmount - balance)

  return {
    ok: deficit <= 0,
    reason: deficit <= 0 ? 'sufficient-balance' : 'insufficient-balance',
    message: deficit <= 0 ? '' : TDX_SMS_RECEIVE_INSUFFICIENT_BALANCE_MESSAGE,
    userId: Number(user.Id || safeUserId),
    username,
    balance,
    price: resolvedPrice,
    requiredAmount,
    deficit,
    needsCharge,
    orderDealId: resolvedOrderDealId,
    orderNumber,
    chargeCount: outstanding.chargeCount,
    refundCount: outstanding.refundCount,
    outstandingCharges: outstanding.outstandingCharges,
  }
}

function isTdxSmsReceiveOrder(order = {}) {
  const companyCode = normalizeText(order.companycode)
  const companyName = normalizeText(order.companyname)
  return companyCode === TDX_SMS_RECEIVE_COMPANY_CODE || companyName.includes(TDX_SMS_RECEIVE_COMPANY_NAME)
}

function shouldChargeOrder(order = {}) {
  const orderState = Number(order.orderstate || 0)
  const queryres = normalizeText(order.queryres)
  if ([33, 333, 6, 55, 666].includes(orderState)) return true
  return ['TokenSaved', 'SessionRetrying', 'AppealSubmitted', 'AppealCompleted', 'AppealResubmitted', 'AutoCheckCompleted'].includes(queryres)
}

function shouldRefundOrder(order = {}) {
  const orderState = Number(order.orderstate || 0)
  const queryres = normalizeText(order.queryres)
  if (orderState === 777) return true
  return [
    'AppealFailed',
    'AppealRefunded',
    'AppealResubmitFailed',
    'AppealResubmitRefunded',
    'AutoCheckFailed',
    'AutoCheckRefunded',
  ].includes(queryres)
}

function buildChargeNote(order = {}) {
  const orderNumber = normalizeText(order.orderpicinumber)
  const phone = normalizeText(order.phone)
  return `关于${TDX_SMS_RECEIVE_ORDER_TYPE}的订单_picinumber${orderNumber}_${phone}`
}

function buildRefundNote(order = {}) {
  const orderNumber = normalizeText(order.orderpicinumber)
  const phone = normalizeText(order.phone)
  return `关于${TDX_SMS_RECEIVE_ORDER_TYPE}的订单退款_picinumber${orderNumber}_${phone}`
}

async function insertMoneyRecord(connection, {
  userId,
  username,
  beforeAmount,
  delta,
  afterAmount,
  type,
  orderNumber,
  phone = '',
  note = '',
}) {
  await connection.query(
    `INSERT INTO t_money (userid, username, \`before\`, money, \`after\`, type, ordernumber, ordertype, phone, note, CreateOn, UpdateOn, CreateBy, UpdateBy, createtime)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), ?, ?, NOW())`,
    [
      Number(userId),
      username,
      beforeAmount,
      delta,
      afterAmount,
      Number(type),
      orderNumber,
      TDX_SMS_RECEIVE_ORDER_TYPE,
      phone,
      note,
      Number(userId),
      Number(userId),
    ],
  )
}

export async function reconcileTdxSmsReceiveBillingByOrderId(orderDealId = 0, options = {}) {
  const id = Number(orderDealId || 0)
  if (!id) {
    return { ok: false, action: 'noop', reason: 'invalid-order-id' }
  }

  return withTransaction(async (connection) => {
    const [orderRows] = await connection.query(
      `SELECT Id, userid, username, phone, companycode, companyname, queryres, orderstate, orderpicinumber, orderprice
         FROM t_orderdeal
        WHERE Id = ?
        LIMIT 1
        FOR UPDATE`,
      [id],
    )
    const order = orderRows[0]
    if (!order) {
      return { ok: false, action: 'noop', reason: 'order-not-found', orderDealId: id }
    }
    if (!isTdxSmsReceiveOrder(order)) {
      return { ok: true, action: 'noop', reason: 'not-tdx-sms-receive', orderDealId: id }
    }

    const userId = Number(order.userid || 0)
    const orderNumber = normalizeText(order.orderpicinumber)
    const amount = toPositiveAmount(order.orderprice)
    if (!userId || !orderNumber || amount <= 0) {
      return {
        ok: true,
        action: 'noop',
        reason: 'missing-billing-context',
        orderDealId: id,
        userId,
        amount,
      }
    }

    const [userRows] = await connection.query(
      'SELECT Id, UserName, bal FROM t_user WHERE Id = ? LIMIT 1 FOR UPDATE',
      [userId],
    )
    const user = userRows[0]
    if (!user) {
      return { ok: false, action: 'noop', reason: 'user-not-found', orderDealId: id, userId }
    }

    const [moneyRows] = await connection.query(
      `SELECT Id, type, money
         FROM t_money
        WHERE userid = ?
          AND ordernumber = ?
          AND ordertype = ?
        ORDER BY Id ASC
        FOR UPDATE`,
      [userId, orderNumber, TDX_SMS_RECEIVE_ORDER_TYPE],
    )

    const { chargeCount, refundCount, outstandingCharges } = summarizeOutstandingCharges(moneyRows)

    const currentBalance = parseMoney(user.bal, 0)
    const username = normalizeText(order.username) || normalizeText(user.UserName)
    const phone = normalizeText(order.phone)

    if (shouldChargeOrder(order) && outstandingCharges <= 0) {
      if (currentBalance < amount) {
        return {
          ok: false,
          action: 'blocked',
          reason: 'insufficient-balance',
          message: TDX_SMS_RECEIVE_INSUFFICIENT_BALANCE_MESSAGE,
          orderDealId: id,
          orderNumber,
          userId,
          amount,
          beforeAmount: currentBalance,
          afterAmount: currentBalance,
          requiredAmount: amount,
          deficit: amount - currentBalance,
          chargeCount,
          refundCount,
        }
      }
      const nextBalance = currentBalance - amount
      await connection.query('UPDATE t_user SET bal = ?, UpdateOn = NOW() WHERE Id = ?', [nextBalance, userId])
      await insertMoneyRecord(connection, {
        userId,
        username,
        beforeAmount: currentBalance,
        delta: -amount,
        afterAmount: nextBalance,
        type: TDX_SMS_RECEIVE_CHARGE_TYPE,
        orderNumber,
        phone,
        note: buildChargeNote(order),
      })
      return {
        ok: true,
        action: 'charged',
        reason: options.reason || 'submitted',
        orderDealId: id,
        orderNumber,
        userId,
        amount,
        beforeAmount: currentBalance,
        afterAmount: nextBalance,
      }
    }

    if (shouldRefundOrder(order) && outstandingCharges > 0) {
      const nextBalance = currentBalance + amount
      await connection.query('UPDATE t_user SET bal = ?, UpdateOn = NOW() WHERE Id = ?', [nextBalance, userId])
      await insertMoneyRecord(connection, {
        userId,
        username,
        beforeAmount: currentBalance,
        delta: amount,
        afterAmount: nextBalance,
        type: TDX_SMS_RECEIVE_REFUND_TYPE,
        orderNumber,
        phone,
        note: buildRefundNote(order),
      })
      return {
        ok: true,
        action: 'refunded',
        reason: options.reason || 'failed',
        orderDealId: id,
        orderNumber,
        userId,
        amount,
        beforeAmount: currentBalance,
        afterAmount: nextBalance,
      }
    }

    return {
      ok: true,
      action: 'noop',
      reason: shouldChargeOrder(order)
        ? 'already-charged'
        : shouldRefundOrder(order)
          ? 'nothing-to-refund'
          : 'not-billable-state',
      orderDealId: id,
      orderNumber,
      userId,
      amount,
      chargeCount,
      refundCount,
    }
  })
}
