/**
 * 文件：frontend/tests/e2e/home-world.spec.js
 * 所属模块：Playwright 端到端测试
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
import {expect, test} from '@playwright/test'

test.beforeEach(async ({page}) => {
  await page.addInitScript(() => {
    sessionStorage.setItem('__disable_web_vitals__', '1')
  })
})

test('首页工具栏、分类排序和分类内容卡片', async ({page}) => {
  await page.goto('/#/')
  await expect(page.locator('.home-bottom-dock')).toBeVisible()
  await expect(page.locator('.dock-item')).toHaveCount(5)

  await page.locator('.tab-item').filter({hasText: '灵感分类'}).click()
  await page.locator('.category-card').first().click()
  await expect(page.locator('.subcategory-hero')).toBeVisible()

  await page.locator('.sub-tag').first().click()
  await expect(page.locator('.category-sort-bar')).toBeVisible()
  await expect(page.locator('.category-sort-bar button')).toHaveCount(2)
  await expect(page.locator('.category-sort-bar button').filter({hasText: '最新'})).toBeVisible()
  await expect(page.locator('.category-sort-bar button').filter({hasText: '热度'})).toBeVisible()
  await expect(page.locator('.category-sort-bar button').filter({hasText: '推荐'})).toHaveCount(0)

  const timeRequest = page.waitForRequest(request =>
    request.url().includes('/api/inspire/public/list') && request.url().includes('sort=time')
  )
  await page.locator('.category-sort-bar button').filter({hasText: '最新'}).click()
  await timeRequest
  await expect(page.locator('.category-inspire-card').first()).toBeVisible()
})

test('世界种子公开列表、分支和章节懒加载入口', async ({page}) => {
  await page.goto('/#/world')
  await expect(page.locator('.seed-card').first()).toBeVisible()

  await page.locator('.seed-card').first().click()
  await expect(page.locator('.line-tabs button').first()).toBeVisible()
  await expect(page.locator('.branch-tabs button').first()).toBeVisible()

  const branchButtons = page.locator('.branch-tabs button')
  const count = await branchButtons.count()
  if (count > 1) {
    await branchButtons.nth(1).click()
    const chapterCards = page.locator('.chapter-card')
    if (await chapterCards.count()) {
      await expect(page.locator('.chapter-toggle').first()).toBeVisible()
      await page.locator('.chapter-toggle').first().click()
      await expect(page.locator('.chapter-body').first()).toBeVisible()
    }
  }
})
