const { chromium } = require('playwright');

(async () => {
    console.log('╔════════════════════════════════════════════════════════╗');
    console.log('║       AI工具广场 - 登录后点赞评论测试                  ║');
    console.log('╚════════════════════════════════════════════════════════╝\n');
    
    const browser = await chromium.launch({ headless: true });
    const context = await browser.newContext();
    const page = await context.newPage();
    
    // 1. Login first
    console.log('[1/8] 导航到登录页面...');
    await page.goto('http://localhost:5174/login', { timeout: 30000 });
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    
    console.log('[2/8] 填写登录信息...');
    await page.fill('input[placeholder*="用户"], input[placeholder*="username"], input[name="username"]', 'test_autobot');
    await page.fill('input[type="password"]', 'Test123456');
    
    console.log('[3/8] 点击登录按钮...');
    await page.click('button:has-text("登录"), button:has-text("登入"), button[type="submit"]');
    await page.waitForTimeout(3000);
    
    // Check if logged in
    const bodyText = await page.textContent('body');
    const isLoggedIn = bodyText.includes('test_autobot') || bodyText.includes('登出') || bodyText.includes('退出');
    console.log(`  登录状态: ${isLoggedIn ? '✅ 已登录' : '⚠️ 可能未登录'}`);
    
    // 4. Navigate to tool detail page
    console.log('\n[4/8] 导航到工具详情页...');
    await page.goto('http://localhost:5174/tools/1', { timeout: 30000 });
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(3000);
    
    console.log('[5/8] 检查页面元素...');
    const pageContent = await page.textContent('body');
    
    if (pageContent.includes('ssh-mcp')) {
        console.log('  ✅ 工具名称显示正常');
    }
    
    if (pageContent.includes('MCP')) {
        console.log('  ✅ 分类显示正常');
    }
    
    // 5. Test like button
    console.log('\n[6/8] 测试点赞按钮...');
    const likeBtn = page.locator('.like-btn').first();
    if (await likeBtn.count() > 0) {
        console.log('  ✅ 找到点赞按钮');
        
        // Get initial like count
        const initialCount = await likeBtn.textContent();
        console.log(`  初始点赞数: ${initialCount}`);
        
        // Click like button
        await likeBtn.click();
        await page.waitForTimeout(2000);
        
        const afterLikeCount = await likeBtn.textContent();
        console.log(`  点击后点赞数: ${afterLikeCount}`);
        
        // Check if like status changed
        if (initialCount !== afterLikeCount || await likeBtn.getAttribute('class').then(c => c?.includes('liked'))) {
            console.log('  ✅ 点赞功能正常');
        } else {
            console.log('  ⚠️  点赞数可能未改变(可能已经点过了)');
        }
    } else {
        console.log('  ⚠️  未找到点赞按钮');
    }
    
    // 6. Test comment
    console.log('\n[7/8] 测试评论功能...');
    const textarea = page.locator('.comment-editor textarea, .content-input').first();
    if (await textarea.count() > 0) {
        console.log('  ✅ 找到评论输入框');
        
        // Clear and type comment
        await textarea.clear();
        await textarea.fill('自动化浏览器测试评论 - 验证登录后评论功能');
        
        // Click submit button
        const submitBtn = page.locator('button:has-text("发送"), .submit-btn').first();
        if (await submitBtn.count() > 0) {
            await submitBtn.click();
            await page.waitForTimeout(2000);
            console.log('  ✅ 已提交评论');
        } else {
            console.log('  ⚠️  未找到发送按钮');
        }
    } else {
        console.log('  ⚠️  未找到评论输入框');
    }
    
    // 7. Check comment list
    console.log('\n[8/8] 检查评论列表...');
    const commentSection = await page.textContent('.comments-section, [class*="comment"]');
    if (commentSection?.includes('自动化') || commentSection?.includes('test_autobot')) {
        console.log('  ✅ 评论显示正常');
    } else {
        console.log('  ⚠️  评论可能还在加载或需要滚动');
    }
    
    // Take screenshot
    await page.screenshot({ path: '/tmp/login-test-final.png', fullPage: true });
    console.log('\n  截图已保存到 /tmp/login-test-final.png');
    
    await browser.close();
    
    console.log('\n╔════════════════════════════════════════════════════════╗');
    console.log('║                    测试完成                             ║');
    console.log('╚════════════════════════════════════════════════════════╝');
})();
