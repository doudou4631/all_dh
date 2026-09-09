import assert from 'node:assert/strict'
import test from 'node:test'
import { validateLiveTdxConfig } from '../src/services/tdxCleanupService.js'

const smsReadyConfig = {
  mode: 'live',
  originalIp: '127.0.0.1',
  sendSmsUrl: 'https://example.test/send-sms',
  loginSmsUrl: 'https://example.test/login-sms',
  answersStartUrl: 'https://example.test/answers/start',
  checkAppletPhoneUrl: 'https://example.test/check-phone',
  answersSubmitUrl: 'https://example.test/answers/submit',
  queryProgressUrl: 'https://example.test/progress',
}

test('allows live precheck and SMS sending before an appeal channel is configured', () => {
  assert.deepEqual(
    validateLiveTdxConfig(smsReadyConfig, { requireAppealChannel: false }),
    [],
  )
})

test('still requires QMF or DF for the complete appeal flow', () => {
  const problems = validateLiveTdxConfig(smsReadyConfig)

  assert.ok(problems.some((problem) => problem.includes('QMF')))
  assert.ok(problems.some((problem) => problem.includes('登峰')))
})
