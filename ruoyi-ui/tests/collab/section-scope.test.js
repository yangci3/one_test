const assert = require('assert')
const fs = require('fs')
const path = require('path')

const scope = require('../../src/utils/collab/sectionScope')

function run() {
  const html = '<div data-sec-id="sec-1"><p>one</p></div>'
    + '<div data-sec-id="sec-2"><p>two</p></div>'

  assert.deepStrictEqual(scope.listSectionIds(html), ['sec-1', 'sec-2'])
  assert.deepStrictEqual(scope.listSectionIds(''), [])
  assert.deepStrictEqual(scope.listSectionIds(null), [])

  const extracted = scope.extractEditableHtml(html, ['sec-1'])
  assert.ok(extracted.includes('data-sec-id="sec-1"'))
  assert.ok(extracted.includes('<p>one</p>'))
  assert.ok(!extracted.includes('sec-2'))
  assert.ok(!extracted.includes('<p>two</p>'))

  assert.deepStrictEqual(scope.extractEditableHtml(html, []), '')
  assert.strictEqual(scope.extractEditableHtml(null, ['sec-1']), '')

  assert.deepStrictEqual(
    scope.parseScopeJson('{"sectionIds":["sec-1","sec-2"]}'),
    ['sec-1', 'sec-2']
  )
  assert.deepStrictEqual(scope.parseScopeJson({ sectionIds: ['sec-3'] }), ['sec-3'])
  assert.deepStrictEqual(scope.parseScopeJson(''), [])
  assert.deepStrictEqual(scope.parseScopeJson(null), [])
  assert.deepStrictEqual(scope.parseScopeJson('not-json'), [])
  assert.deepStrictEqual(scope.parseScopeJson('{"fromSec":"sec-1"}'), [])

  const nested = '<div data-sec-id="sec-a"><div><p>nested</p></div></div>'
    + '<div data-sec-id="sec-b"><p>b</p></div>'
  const nestedExtract = scope.extractEditableHtml(nested, ['sec-a'])
  assert.ok(nestedExtract.includes('<p>nested</p>'))
  assert.ok(!nestedExtract.includes('sec-b'))

  console.log('section-scope.test.js PASS')
}

run()
