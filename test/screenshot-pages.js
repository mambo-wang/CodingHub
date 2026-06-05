const { chromium } = require('playwright');

(async () => {
    const browser = await chromium.launch({ headless: true });
    const page = await browser.newPage();
    
    console.log('=== 快速开始页面 ===');
    await page.goto('http://localhost:5174/quickstart', { timeout: 30000 });
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    await page.screenshot({ path: '/tmp/quickstart-page.png', fullPage: true });
    console.log('截图保存到 /tmp/quickstart-page.png');
    
    // Get page title
    const title = await page.title();
    console.log('页面标题:', title);
    
    // Get some text content to verify
    const content = await page.textContent('body');
    if (content.includes('MCP')) {
        console.log('✅ 页面包含 MCP 内容');
    }
    if (content.includes('快速开始')) {
        console.log('✅ 页面包含"快速开始"标题');
    }
    
    console.log('\n=== 关于页面 ===');
    await page.goto('http://localhost:5174/about', { timeout: 30000 });
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    await page.screenshot({ path: '/tmp/about-page.png', fullPage: true });
    console.log('截图保存到 /tmp/about-page.png');
    
    const aboutContent = await page.textContent('body');
    if (aboutContent.includes('AI 工具广场')) {
        console.log('✅ 页面包含"AI 工具广场"');
    }
    if (aboutContent.includes('技术栈')) {
        console.log('✅ 页面包含 README 内容');
    }
    
    await browser.close();
    console.log('\n截图完成!');
})();
