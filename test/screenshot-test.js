const { chromium } = require('playwright');

(async () => {
    console.log('==========================================');
    console.log('CodingHub - 工具详情页测试');
    console.log('==========================================\n');
    
    const browser = await chromium.launch({ headless: true });
    const page = await browser.newPage();
    
    console.log('[1/6] 导航到工具详情页...');
    // Correct URL: /tools/1 not /tool/1
    await page.goto('http://localhost:5174/tools/1', { timeout: 30000 });
    
    console.log('[2/6] 等待内容加载...');
    await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {});
    await page.waitForTimeout(3000);
    
    console.log('  当前URL:', page.url());
    
    // Get body text
    const bodyText = await page.textContent('body');
    
    console.log('\n[3/6] 检查页面元素...');
    const checks = [
        { name: '点赞按钮', pattern: /赞|点赞|like|Like|点赞/i },
        { name: '评论区域', pattern: /评论|comment|Comment|发表评论/i },
        { name: '工具名称', pattern: /ssh-mcp/i },
        { name: 'MCP分类', pattern: /MCP|类别/i },
        { name: '工具内容', pattern: /sfsd|内容/i }
    ];
    
    let passed = 0;
    for (const check of checks) {
        if (check.pattern.test(bodyText)) {
            console.log(`  ✅ 找到: ${check.name}`);
            passed++;
        } else {
            console.log(`  ⚠️  未找到: ${check.name}`);
        }
    }
    
    console.log('\n[4/6] 测试API连接...');
    const apiResult = await page.evaluate(async () => {
        try {
            const resp = await fetch('/api/v1/tools/1/like-status');
            const data = await resp.json();
            return { success: true, data };
        } catch (e) {
            return { success: false, error: e.message };
        }
    });
    
    if (apiResult.success) {
        console.log('  ✅ like-status API响应正常:', JSON.stringify(apiResult.data));
        passed++;
    } else {
        console.log('  ❌ like-status API请求失败:', apiResult.error);
    }
    
    console.log('\n[5/6] 测试评论API...');
    const commentsResult = await page.evaluate(async () => {
        try {
            const resp = await fetch('/api/v1/tools/1/comments');
            const data = await resp.json();
            return { success: true, data };
        } catch (e) {
            return { success: false, error: e.message };
        }
    });
    
    if (commentsResult.success) {
        console.log('  ✅ comments API响应正常');
        passed++;
    } else {
        console.log('  ❌ comments API请求失败:', commentsResult.error);
    }
    
    console.log('\n[6/6] 保存截图...');
    await page.screenshot({ path: '/tmp/tool-detail-test.png', fullPage: true });
    console.log('  截图已保存到 /tmp/tool-detail-test.png');
    
    await browser.close();
    
    console.log('\n==========================================');
    console.log(`测试结果: ${passed}/7 通过`);
    console.log('==========================================');
    
    if (passed >= 5) {
        console.log('✅ 工具详情页功能基本正常!');
    } else {
        console.log('❌ 部分功能异常，需要检查');
    }
})();
