import assert from 'node:assert/strict'
import test from 'node:test'
import { readFile } from 'node:fs/promises'
import { parse } from '@vue/compiler-sfc'
import { effectScope, nextTick, reactive, ref } from 'vue'
import { INSPECTION_SCOPE_OPTIONS, inspectionDefaults, inspectionForm, inspectionRunSummary, inspectionScopeLabel } from '../src/utils/markInspectionPolicy.js'
import { WUMA_PLATFORM_CODES } from '../src/utils/markWumaPlatform.js'

const source = parse(await readFile(new URL('../src/views/server/mark/inspection/policy.vue', import.meta.url), 'utf8')).descriptor.scriptSetup.content.replace(/^import .*$/gm, '')
const exposed = 'form, dialog, runningIds, openCreate, openEdit, changePlatform, changeScope, changeTakeover, submit, runNow'
const deferred = () => { let resolve, reject; const promise = new Promise((yes, no) => { resolve = yes; reject = no }); return { promise, resolve, reject } }
async function page(t, { confirm = async () => {}, run = async () => ({ data: {} }) } = {}) {
  const scope = effectScope(); t.after(() => scope.stop())
  const creates = [], updates = [], runs = [], confirmations = [], messages = []
  const deps = {
    reactive, ref, INSPECTION_SCOPE_OPTIONS, inspectionDefaults, inspectionForm, inspectionRunSummary, inspectionScopeLabel, WUMA_PLATFORM_CODES,
    getCurrentInstance: () => ({ proxy: {
      $refs: { formRef: { validate: cb => cb(true) } },
      $modal: {
        confirm: async message => { confirmations.push(message); return confirm(message) },
        msgSuccess: message => messages.push({ type: 'success', message }), msgWarning: message => messages.push({ type: 'warning', message })
      }
    } }),
    listInspectionPlatforms: async () => ({ data: [] }), listInspectionPolicies: async () => ({ rows: [], total: 0 }),
    addInspectionPolicy: async p => { creates.push(JSON.parse(JSON.stringify(p))); return {} },
    updateInspectionPolicy: async p => { updates.push(JSON.parse(JSON.stringify(p))); return {} },
    deleteInspectionPolicy: async () => ({}), runInspectionPolicy: async id => { runs.push(id); return run(id) }
  }
  const p = scope.run(() => new Function(...Object.keys(deps), `${source}\nreturn {${exposed}}`)(...Object.values(deps)))
  await nextTick()
  return { ...p, creates, updates, runs, confirmations, messages }
}

for (const code of WUMA_PLATFORM_CODES) {
  test(`${code}: new policy saves both states and automatic takeover`, async t => {
    const p = await page(t)
    p.openCreate(); p.form.platformCode = code; p.changePlatform(code)
    p.form.policyName = '无码巡检'; p.form.status = '0'
    p.submit(); await nextTick()
    assert.equal(p.creates[0].orderScope, 'BOTH')
    assert.equal(p.creates[0].autoTakeover, true)
    assert.equal(p.creates[0].status, '0')
    assert.equal(p.dialog.value, false)
  })
}

test('editing a saved disabled takeover retains false and does not mutate the list row', async t => {
  const p = await page(t)
  const row = { id: 3, platformCode: 'wuma_360', orderScope: 'PROCESSING', autoTakeover: false }
  p.openEdit(row)
  assert.equal(p.form.autoTakeover, false)
  p.form.policyName = 'Edited'; p.submit(); await nextTick()
  assert.equal(p.updates[0].autoTakeover, false)
  assert.equal(row.policyName, undefined)
})

test('turning off takeover switches Wuma to processing only and enabling it restores both', async t => {
  const p = await page(t)
  p.form.platformCode = 'wuma_360'; p.changePlatform('wuma_360')
  p.form.autoTakeover = false; p.changeTakeover(false)
  assert.equal(p.form.orderScope, 'PROCESSING')
  p.form.autoTakeover = true; p.changeTakeover(true)
  assert.equal(p.form.orderScope, 'BOTH')
  p.form.autoTakeover = false; p.form.orderScope = 'PENDING'; p.changeScope('PENDING')
  assert.equal(p.form.autoTakeover, true)
})

test('changing platform resets automatic defaults for specialized queues', async t => {
  const p = await page(t)
  p.changePlatform('wuma_360'); p.changePlatform('td_gaopin')
  assert.equal(p.form.orderScope, 'PENDING'); assert.equal(p.form.autoTakeover, false)
})

test('manual run blocks repeated clicks while confirming and reports actual detection counts', async t => {
  const confirmation = deferred(), execution = deferred()
  const p = await page(t, { confirm: () => confirmation.promise, run: () => execution.promise })
  const row = { id: 3, policyName: '360无码', platformCode: 'wuma_360', orderScope: 'BOTH', autoTakeover: true, autoAction: 'SUCCESS', status: '0', running: '0' }
  const first = p.runNow(row)
  await p.runNow(row)
  assert.equal(p.confirmations.length, 1); assert.equal(p.runs.length, 0)
  assert.match(p.confirmations[0], /待处理及处理中/)
  confirmation.resolve(); await nextTick(); await p.runNow(row)
  assert.equal(p.runs.length, 1); assert.equal(p.runningIds.has(3), true)
  execution.resolve({ data: { scannedCount: 4, checkedCount: 4, successCount: 1, markedCount: 2, failedCount: 1 } })
  await first
  assert.equal(p.runningIds.has(3), false)
  assert.equal(p.messages[0].type, 'warning')
  assert.match(p.messages[0].message, /实际检测 4 条，已处理 1 条，仍有标记 2 条，检测失败 1 条/)
})

test('failed manual request clears loading for a retry', async t => {
  let attempts = 0
  const p = await page(t, { run: async () => { if (!attempts++) throw Error('timeout'); return { data: {} } } })
  const row = { id: 3, status: '0', orderScope: 'BOTH' }
  await assert.rejects(p.runNow(row), /timeout/)
  assert.equal(p.runningIds.has(3), false)
  await p.runNow(row); assert.equal(p.runs.length, 2)
})

test('paused or running policies cannot be manually started', async t => {
  const p = await page(t)
  await p.runNow({ id: 3, status: '1', running: '0' })
  await p.runNow({ id: 4, status: '0', running: '1' })
  assert.equal(p.confirmations.length, 0); assert.equal(p.runs.length, 0)
})
