import assert from 'node:assert/strict'
import test from 'node:test'
import { createSession, getSession, removeSession } from '../src/lib/auth.js'

test('creates a verifiable session with an expiry', () => {
  const created = createSession({ username: 'tester' })
  const session = getSession(created.token)

  assert.equal(session.user.username, 'tester')
  assert.ok(Date.parse(session.expiresAt) > Date.parse(session.issuedAt))
})

test('rejects tampered and expired sessions', () => {
  const created = createSession({ username: 'tester' })
  const tampered = `${created.token.slice(0, -1)}x`
  const expired = createSession(
    { username: 'tester' },
    { now: new Date(Date.now() - 10 * 60 * 1000), ttlSeconds: 300 },
  )

  assert.equal(getSession(tampered), null)
  assert.equal(getSession(expired.token), null)
})

test('revokes a session on logout', () => {
  const created = createSession({ username: 'tester' })

  assert.equal(removeSession(created.token), true)
  assert.equal(getSession(created.token), null)
  assert.equal(removeSession('invalid-token'), false)
})
