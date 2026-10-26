import assert from 'node:assert/strict'
import test from 'node:test'
import { resolveMarkPlatformCodeFromRoute } from '../src/utils/markPlatformRoute.js'
import { WUMA_PLATFORM_CODES } from '../src/utils/markWumaPlatform.js'

const platforms = [
  { code: 'wuma_taidixiong', suffix: 'Taidixiong', title: 'Taidixiong无码' },
  { code: 'wuma_tengxun', suffix: 'Tengxun', title: 'Tengxun无码' },
  { code: 'wuma_360', suffix: '360', title: '360无码' }
]

for (const { code, suffix, title } of platforms) {
  for (const role of ['User', 'Agent']) {
    const path = role === 'User' ? `wuma${suffix}` : `agentWuma${suffix}`
    const name = `Mark${role}Wuma${suffix}`
    const allowed = role === 'User' ? WUMA_PLATFORM_CODES : []

    test(`${role} ${code}: resolves a direct menu URL without query parameters`, () => {
      assert.equal(resolveMarkPlatformCodeFromRoute({ path: `/mark/${path}`, query: {} }, allowed), code)
    })

    test(`${role} ${code}: resolves the menu route name`, () => {
      assert.equal(resolveMarkPlatformCodeFromRoute({ name, query: {} }, allowed), code)
    })

    test(`${role} ${code}: menu path takes priority over a stale platform query`, () => {
      assert.equal(resolveMarkPlatformCodeFromRoute({
        path: `/mark/${path}`, query: { platformCode: 'wuma_360' }
      }, allowed), code)
    })
  }

  test(`${code}: resolves the menu title when other route metadata is missing`, () => {
    assert.equal(resolveMarkPlatformCodeFromRoute({ meta: { title } }, WUMA_PLATFORM_CODES), code)
  })

  test(`${code}: keeps support for existing platform query parameters`, () => {
    assert.equal(resolveMarkPlatformCodeFromRoute({ query: { platformCode: code } }, WUMA_PLATFORM_CODES), code)
    assert.equal(resolveMarkPlatformCodeFromRoute({ query: JSON.stringify({ platformCode: code }) }, WUMA_PLATFORM_CODES), code)
  })
}

test('Wuma pages reject ordinary platforms and unknown routes', () => {
  assert.equal(resolveMarkPlatformCodeFromRoute({ path: '/mark/tencentMark' }, WUMA_PLATFORM_CODES), '')
  assert.equal(resolveMarkPlatformCodeFromRoute({ path: '/mark/unknown' }, WUMA_PLATFORM_CODES), '')
  assert.equal(resolveMarkPlatformCodeFromRoute({ query: { platformCode: 'tengxun' } }, WUMA_PLATFORM_CODES), '')
})

test('Wuma platform lookup respects the caller allowed platform list', () => {
  assert.equal(resolveMarkPlatformCodeFromRoute({ path: '/mark/wumaTaidixiong' }, ['tengxun']), '')
})

test('existing ordinary platform routes keep their precedence and aliases', () => {
  assert.equal(resolveMarkPlatformCodeFromRoute({ path: '/mark/tencentMark', query: { platformCode: 'wuma_360' } }), 'tengxun')
  assert.equal(resolveMarkPlatformCodeFromRoute({ name: 'MarkUserTdSecond' }), 'td_second')
  assert.equal(resolveMarkPlatformCodeFromRoute({ query: { platformCode: '360' } }), '360fugai')
})
