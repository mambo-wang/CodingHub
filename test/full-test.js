const { chromium } = require('playwright');

const BASE_URL = 'http://localhost:5174';
const API_BASE = 'http://localhost:8082/api/v1';

async function runFullTests() {
    console.log('╔══════════════════════════════════════════════════════╗');
    console.log('║       CodingHub - 完整自动化测试套件                  ║');
    console.log('╚══════════════════════════════════════════════════════╝\n');

    const browser = await chromium.launch({ headless: true });
    const context = await browser.newContext();
    const page = await context.newPage();
    
    let totalTests = 0;
    let passedTests = 0;
    let failedTests = 0;

    const results = [];

    // Helper function
    const test = async (name, fn) => {
        totalTests++;
        try {
            await fn();
            results.push({ name, status: 'PASS' });
            passedTests++;
            console.log(`  ✅ ${name}`);
        } catch (e) {
            results.push({ name, status: 'FAIL', error: e.message });
            failedTests++;
            console.log(`  ❌ ${name}: ${e.message}`);
        }
    };

    // ========== API Tests ==========
    console.log('📡 API测试\n');
    console.log('--- 未认证接口 ---');
    
    await test('GET /tools 返回工具列表', async () => {
        const resp = await page.evaluate(async (url) => {
            const r = await fetch(url);
            return { status: r.status, data: await r.json() };
        }, `${API_BASE}/tools`);
        if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`返回${resp.status}`);
    });

    await test('GET /tools/{id} 返回工具详情', async () => {
        const resp = await page.evaluate(async (url) => {
            const r = await fetch(url);
            return { status: r.status, data: await r.json() };
        }, `${API_BASE}/tools/1`);
        if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`返回${resp.status}`);
    });

    await test('GET /tools/{id}/like-status (公开接口)', async () => {
        const resp = await page.evaluate(async (url) => {
            const r = await fetch(url);
            return { status: r.status, data: await r.json() };
        }, `${API_BASE}/tools/1/like-status`);
        if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`返回${resp.status}`);
    });

    await test('GET /tools/{id}/comments (公开接口)', async () => {
        const resp = await page.evaluate(async (url) => {
            const r = await fetch(url);
            return { status: r.status, data: await r.json() };
        }, `${API_BASE}/tools/1/comments`);
        if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`返回${resp.status}`);
    });

    await test('GET /categories 返回分类', async () => {
        const resp = await page.evaluate(async (url) => {
            const r = await fetch(url);
            return { status: r.status, data: await r.json() };
        }, `${API_BASE}/categories`);
        if (resp.status !== 200 || resp.data.code !== 200) throw new Error(`返回${resp.status}`);
    });

    // ========== UI Tests ==========
    console.log('\n🖥️ 前端UI测试\n');

    await test('工具详情页加载正常', async () => {
        await page.goto(`${BASE_URL}/tools/1`, { timeout: 30000 });
        await page.waitForLoadState('networkidle');
        await page.waitForTimeout(2000);
        
        const url = page.url();
        if (!url.includes('/tools/1')) throw new Error(`URL跳转到${url}`);
    });

    await test('页面显示工具名称', async () => {
        const text = await page.textContent('body');
        if (!text.includes('ssh-mcp')) throw new Error('未找到工具名称');
    });

    await test('页面显示MCP分类', async () => {
        const text = await page.textContent('body');
        if (!text.includes('MCP')) throw new Error('未找到分类');
    });

    await test('找到评论编辑器', async () => {
        const textarea = await page.locator('textarea').count();
        const commentEditor = await page.locator('.comment-editor').count();
        if (textarea === 0 && commentEditor === 0) throw new Error('未找到评论编辑器');
    });

    await test('找到点赞按钮', async () => {
        const likeBtn = await page.locator('.like-btn').count();
        const thumbsUp = await page.locator('[class*="like"]').count();
        if (likeBtn === 0 && thumbsUp === 0) throw new Error('未找到点赞按钮');
    });

    await test('点赞按钮可点击', async () => {
        const btn = page.locator('.like-btn').first();
        const count = await btn.count();
        if (count > 0) {
            const isDisabled = await btn.isDisabled();
            // If not disabled, button should be clickable
            if (!isDisabled) {
                await btn.click();
                await page.waitForTimeout(500);
            }
        }
    });

    await test('评论输入框可输入', async () => {
        const textarea = page.locator('.comment-editor textarea, .content-input').first();
        const count = await textarea.count();
        if (count > 0) {
            await textarea.fill('自动化测试评论内容');
            const value = await textarea.inputValue();
            if (!value.includes('自动化测试')) throw new Error('输入框无法输入');
        }
    });

    await test('评论发送按钮存在', async () => {
        const sendBtn = await page.locator('button:has-text("发送"), .submit-btn').count();
        if (sendBtn === 0) throw new Error('未找到发送按钮');
    });

    await test('工具内容显示正常', async () => {
        const content = await page.locator('.tool-content, .markdown-body').count();
        if (content === 0) throw new Error('未找到内容区域');
    });

    // ========== Error Handling ==========
    console.log('\n⚠️ 错误处理测试\n');

    await test('不存在的工具显示404', async () => {
        await page.goto(`${BASE_URL}/tools/99999`, { timeout: 30000 });
        await page.waitForTimeout(3000);
        const text = await page.textContent('body');
        if (!text.includes('404') && !text.includes('不存在')) {
            throw new Error('应该显示404页面');
        }
    });

    // Take final screenshot
    await page.screenshot({ path: '/tmp/ai-tool-square-test-final.png', fullPage: true });
    
    await browser.close();

    // ========== Summary ==========
    console.log('\n╔══════════════════════════════════════════════════════╗');
    console.log('║                    测试结果汇总                        ║');
    console.log('╚══════════════════════════════════════════════════════╝');
    console.log(`\n  📊 总测试数: ${totalTests}`);
    console.log(`  ✅ 通过: ${passedTests}`);
    console.log(`  ❌ 失败: ${failedTests}`);
    console.log(`\n  截图: /tmp/ai-tool-square-test-final.png`);
    
    if (failedTests === 0) {
        console.log('\n  🎉 所有测试通过!');
    } else {
        console.log('\n  ⚠️  部分测试失败，请检查');
    }

    return failedTests === 0;
}

runFullTests()
    .then(success => {
        process.exit(success ? 0 : 1);
    })
    .catch(err => {
        console.error('\n❌ 测试执行失败:', err);
        process.exit(1);
    });
