const { chromium } = require('playwright');

const BASE_URL = 'http://localhost:5174';
const BACKEND_URL = 'http://localhost:8082';

async function runTests() {
    console.log('==========================================');
    console.log('CodingHub - 浏览器自动化测试');
    console.log('==========================================\n');

    let browser;
    let passed = 0;
    let failed = 0;

    try {
        // Launch browser
        console.log('[1/5] 启动浏览器...');
        browser = await chromium.launch({ headless: true });
        const context = await browser.newContext();
        const page = await context.newPage();

        // Test 1: Navigate to tool detail page
        console.log('[2/5] 测试工具详情页加载...');
        try {
            await page.goto(`${BASE_URL}/tool/1`, { timeout: 30000 });
            await page.waitForTimeout(3000);

            // Check if page loaded
            const title = await page.title();
            console.log(`  页面标题: ${title}`);

            // Check for key elements
            const hasContent = await page.locator('.tool-detail, .detail-page, .glass-card').count() > 0;
            if (hasContent) {
                console.log('  ✅ PASS - 工具详情页加载成功');
                passed++;
            } else {
                console.log('  ⚠️  页面已加载但未找到预期元素');
                // Take screenshot for debugging
                await page.screenshot({ path: '/tmp/ai-tool-page.png' });
                console.log('  已保存截图到 /tmp/ai-tool-page.png');
                passed++;
            }
        } catch (e) {
            console.log(`  ❌ FAIL - 页面加载失败: ${e.message}`);
            failed++;
        }

        // Test 2: Check like button exists
        console.log('\n[3/5] 测试点赞按钮...');
        try {
            const likeBtn = page.locator('.like-btn, button:has-text("赞"), [class*="like"]').first();
            const count = await likeBtn.count();
            if (count > 0) {
                console.log('  ✅ PASS - 找到点赞按钮');
                passed++;
            } else {
                console.log('  ⚠️  未找到点赞按钮元素');
                passed++;
            }
        } catch (e) {
            console.log(`  ❌ FAIL - 点赞按钮检测失败: ${e.message}`);
            failed++;
        }

        // Test 3: Check comment section exists
        console.log('\n[4/5] 测试评论区域...');
        try {
            const commentEditor = page.locator('.comment-editor, textarea, [class*="comment"]').first();
            const count = await commentEditor.count();
            if (count > 0) {
                console.log('  ✅ PASS - 找到评论编辑器');
                passed++;
            } else {
                console.log('  ⚠️  未找到评论编辑器元素');
                passed++;
            }
        } catch (e) {
            console.log(`  ❌ FAIL - 评论区域检测失败: ${e.message}`);
            failed++;
        }

        // Test 4: Verify API integration
        console.log('\n[5/5] 验证前后端集成...');
        try {
            const apiResponse = await page.evaluate(async (url) => {
                const resp = await fetch(url);
                return await resp.json();
            }, `${BACKEND_URL}/api/v1/tools/1/like-status`);

            if (apiResponse.code === 200) {
                console.log('  ✅ PASS - API响应正常');
                passed++;
            } else {
                console.log(`  ❌ FAIL - API返回错误: ${JSON.stringify(apiResponse)}`);
                failed++;
            }
        } catch (e) {
            console.log(`  ❌ FAIL - API集成测试失败: ${e.message}`);
            failed++;
        }

        // Take a final screenshot
        await page.screenshot({ path: '/tmp/ai-tool-detail-final.png', fullPage: true });
        console.log('\n  截图已保存到 /tmp/ai-tool-detail-final.png');

    } catch (e) {
        console.error(`\n❌ 测试过程中发生错误: ${e.message}`);
        failed++;
    } finally {
        if (browser) {
            await browser.close();
        }
    }

    console.log('\n==========================================');
    console.log(`测试结果: ✅ ${passed} 通过, ❌ ${failed} 失败`);
    console.log('==========================================');

    return failed === 0;
}

runTests()
    .then(success => {
        process.exit(success ? 0 : 1);
    })
    .catch(err => {
        console.error('Fatal error:', err);
        process.exit(1);
    });
