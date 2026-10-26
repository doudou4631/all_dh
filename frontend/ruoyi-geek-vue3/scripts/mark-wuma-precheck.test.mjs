import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'
import { parse } from '@vue/compiler-sfc'
import { computed, effectScope, nextTick, reactive, ref, watch } from 'vue'
import { extractPhoneNumbersPreservingOrder, formatExtractedPhones } from '../src/utils/markPhoneExtract.js'
import { resolveMarkPlatformCodeFromRoute } from '../src/utils/markPlatformRoute.js'
import { WUMA_PLATFORM_CODES, WUMA_PLATFORM_NAMES } from '../src/utils/markWumaPlatform.js'

// Run the real page setup with Vue reactivity and mocked HTTP boundaries.
const source = parse(await readFile(new URL('../src/views/server/mark/user/wuma.vue', import.meta.url), 'utf8')).descriptor.scriptSetup.content
const setup = source.replace(/^import .*$/gm, '')
const exposed = 'phonesText, platformCode, checking, submitting, pendingSubmission, confirmOpen, submissionChecked, precheckRows, precheckPassed, precheckError, prepareSubmit, submitOrder'
const phones = ['13800138000', '01012345678']
const marked = () => ({ data: { items: phones.map(phone => ({ phone, querySuccess: true, marked: true, detail: '骚扰电话' })) } })
const deferred = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no }); return { promise, resolve, reject } }

async function page(t, { code = 'wuma_360', precheck = async () => marked(), create } = {}) {
  const scope = effectScope()
  t.after(() => scope.stop())
  const route = reactive({ query: { platformCode: code } })
  const checks = [], orders = [], guards = [], unmounts = []
  const deps = {
    computed, reactive, ref, watch, getCurrentInstance: () => ({ proxy: { $modal: { msgWarning() {}, msgSuccess() {} } } }),
    onBeforeUnmount: fn => unmounts.push(fn), onBeforeRouteLeave: fn => guards.push(fn), onBeforeRouteUpdate: fn => guards.push(fn),
    useRoute: () => route, extractPhoneNumbersPreservingOrder, formatExtractedPhones,
    resolveMarkPlatformCodeFromRoute, WUMA_PLATFORM_CODES, WUMA_PLATFORM_NAMES,
    listMarkUserPlatformPrice: async () => ({ data: WUMA_PLATFORM_CODES.map(platformCode => ({ platformCode, status: '0', remainCount: 100, unitPrice: 2 })) }),
    listMarkUserOrder: async () => ({ rows: [], total: 0 }), getMarkUserOrderDetail: async () => ({}),
    precheckMarkUserOrder: async payload => { checks.push(JSON.parse(JSON.stringify(payload))); return precheck(payload) },
    createMarkUserOrder: async payload => {
      orders.push(JSON.parse(JSON.stringify(payload)))
      return create ? create(payload) : { data: { order: { orderNo: 'test-order', totalCount: 2, totalAmount: 4 } } }
    }
  }
  const state = scope.run(() => new Function(...Object.keys(deps), `${setup}\nreturn { ${exposed} }`)(...Object.values(deps)))
  await nextTick()
  state.phonesText.value = phones.join('\n')
  return { ...state, route, checks, orders, guards, unmounts }
}

for (const code of WUMA_PLATFORM_CODES) {
  test(`${code}: detects every number and requires confirmation before creating an order`, async t => {
    const p = await page(t, { code })
    await p.submitOrder()
    assert.equal(p.orders.length, 0)
    await p.prepareSubmit()
    assert.equal(p.checks[0].platformCode, code)
    assert.deepEqual(p.checks[0].phones, phones)
    assert.equal(p.precheckPassed.value, true)
    assert.equal(p.orders.length, 0)
    await p.submitOrder()
    assert.equal(p.orders.length, 0)
    p.submissionChecked.value = true
    await p.submitOrder()
    assert.equal(p.orders.length, 1)
    assert.equal(p.orders[0].requestNo, p.checks[0].requestNo)
    assert.equal(p.confirmOpen.value, false)
  })
}

for (const [name, result] of [
  ['unmarked', { data: { items: phones.map(phone => ({ phone, querySuccess: true, marked: false })) } }],
  ['mixed failure', { data: { items: [{ phone: phones[0], querySuccess: true, marked: true }, { phone: phones[1], querySuccess: false }] } }],
  ['incomplete', { data: { items: [{ phone: phones[0], querySuccess: true, marked: true }] } }],
  ['duplicate results', { data: { items: Array(2).fill({ phone: phones[0], querySuccess: true, marked: true }) } }],
  ['wrong numbers', { data: { items: ['13900139000', '13900139001'].map(phone => ({ phone, querySuccess: true, marked: true })) } }],
  ['empty', { data: {} }]
]) {
  test(`blocks ${name} precheck without creating orders`, async t => {
    const p = await page(t, { precheck: async () => result })
    await p.prepareSubmit()
    p.submissionChecked.value = true
    await p.submitOrder()
    assert.equal(p.precheckPassed.value, false)
    assert.equal(p.orders.length, 0)
  })
}

test('precheck network failure can be retried', async t => {
  let attempts = 0
  const p = await page(t, { precheck: async () => { if (!attempts++) throw Error('timeout'); return marked() } })
  await p.prepareSubmit()
  assert.match(p.precheckError.value, /检测失败/)
  assert.equal(p.precheckPassed.value, false)
  await p.prepareSubmit()
  assert.equal(p.precheckPassed.value, true)
  assert.equal(p.orders.length, 0)
})

test('blocks duplicate clicks and navigation during detection', async t => {
  const waiting = deferred()
  const p = await page(t, { precheck: () => waiting.promise })
  const first = p.prepareSubmit()
  await p.prepareSubmit()
  assert.equal(p.checks.length, 1)
  assert.equal(p.checking.value, true)
  assert.ok(p.guards.every(guard => guard() === false))
  waiting.resolve(marked())
  await first
  assert.ok(p.guards.every(guard => guard() === true))
})

test('editing numbers invalidates previous results immediately', async t => {
  const p = await page(t)
  await p.prepareSubmit()
  p.submissionChecked.value = true
  p.phonesText.value = '13900139000'
  await p.submitOrder()
  assert.equal(p.precheckPassed.value, false)
  assert.equal(p.confirmOpen.value, false)
  assert.equal(p.orders.length, 0)
})

test('late response after changing platform cannot enable submission', async t => {
  const waiting = deferred()
  const p = await page(t, { precheck: () => waiting.promise })
  const first = p.prepareSubmit()
  p.route.query.platformCode = 'wuma_tengxun'
  await nextTick()
  waiting.resolve(marked())
  await first
  assert.equal(p.precheckPassed.value, false)
  assert.deepEqual(p.precheckRows.value, [])
  assert.equal(p.confirmOpen.value, false)
})

test('uncertain submission retries preserve confirmed payload and requestNo', async t => {
  let attempts = 0
  const p = await page(t, { create: async () => { if (!attempts++) throw Error('network response lost'); return { data: { order: { orderNo: 'existing-order' } } } } })
  await p.prepareSubmit()
  p.submissionChecked.value = true
  await p.submitOrder()
  assert.equal(p.confirmOpen.value, true)
  assert.equal(p.precheckPassed.value, true)
  await p.submitOrder()
  assert.equal(p.orders.length, 2)
  assert.deepEqual(p.orders[0], p.orders[1])
})
