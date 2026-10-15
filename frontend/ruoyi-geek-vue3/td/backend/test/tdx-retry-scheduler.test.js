import assert from 'node:assert/strict'
import test from 'node:test'
import { buildTdxSessionRetrySchedulerErrorPatch } from '../src/routes/tdx.js'
import { compactTdxTaskForStorage } from '../src/data/tdxTaskStore.js'

test('defers an expired session retry task after an unexpected scheduler error', () => {
  const nowMs = 1_800_000_000_000
  const patch = buildTdxSessionRetrySchedulerErrorPatch(
    {
      status: 'processing',
      sessionRetry: {
        status: 'queued',
        attempts: 2,
        maxAttempts: 6,
        nextRetryAtMs: nowMs - 60_000,
      },
    },
    new Error('temporary task-store failure'),
    nowMs,
  )

  assert.equal(patch.status, 'processing')
  assert.equal(patch.sessionRetry.status, 'queued')
  assert.equal(patch.sessionRetry.attempts, 3)
  assert.equal(patch.sessionRetry.lastFailureStep, 'scheduler-error')
  assert.equal(patch.sessionRetry.lastError, 'temporary task-store failure')
  assert.ok(patch.sessionRetry.nextRetryAtMs > nowMs)
})

test('pauses a session retry task on a database schema error', () => {
  const nowMs = 1_800_000_000_000
  const error = Object.assign(new Error("Table 'verifynum.t_user' doesn't exist"), { code: 'ER_NO_SUCH_TABLE' })
  const patch = buildTdxSessionRetrySchedulerErrorPatch(
    { status: 'processing', sessionRetry: { attempts: 1, maxAttempts: 6 } },
    error,
    nowMs,
  )

  assert.equal(patch.status, 'paused')
  assert.equal(patch.currentStep, '后台自动重试已暂停，请修复数据库配置后再恢复。')
  assert.equal(patch.sessionRetry.status, 'paused')
  assert.equal(patch.sessionRetry.nextRetryAtMs, 0)
  assert.equal(patch.sessionRetry.lastFailureStep, 'scheduler-error')
})

test('pauses a session retry task after its maximum attempts', () => {
  const nowMs = 1_800_000_000_000
  const patch = buildTdxSessionRetrySchedulerErrorPatch(
    { status: 'processing', sessionRetry: { attempts: 5, maxAttempts: 6 } },
    new Error('temporary task-store failure'),
    nowMs,
  )

  assert.equal(patch.status, 'paused')
  assert.equal(patch.sessionRetry.status, 'paused')
  assert.equal(patch.sessionRetry.attempts, 6)
  assert.equal(patch.sessionRetry.nextRetryAtMs, 0)
})

test('caps persisted task notes to prevent unbounded JSON growth', () => {
  const notes = Array.from({ length: 105 }, (_, index) => ({ title: `note-${index}` }))
  const compacted = compactTdxTaskForStorage({ id: 'task-1', notes })

  assert.equal(compacted.notes.length, 100)
  assert.equal(compacted.notes[0].title, 'note-0')
  assert.equal(compacted.notes.at(-1).title, 'note-99')
})
