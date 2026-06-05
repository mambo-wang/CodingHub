const { chromium } = require('playwright');

(async () => {
    const browser = await chromium.launch({ headless: true });
    const page = await browser.newPage({ viewport: { width: 1400, height: 900 } });
    
    console.log('=== 快速开始页面 ===');
    await page.goto('http://localhost:5174/quickstart', { timeout: 30000 });
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(3000);
    
    // Scroll down to capture more content
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.waitForTimeout(500);
    
    await page.screenshot({ path: '/tmp/quickstart-full.png', fullPage: true });
    console.log('快速开始截图保存到 /tmp/quickstart-full.png');
    
    const content = await page.textContent('body');
    console.log('内容预览:', content.substring(0, 300).replace(/\s+/g, ' '));
    
    console.log('\n=== 关于页面 ===');
    await page.goto('http://localhost:5174/about', { timeout: 30000 });
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(3000);
    
    await page.screenshot({ path: '/tmp/about-full.png', fullPage: true });
    console.log('关于截图保存到 /tmp/about-full.png');
    
    const aboutContent = await page.textContent('body');
    console.log('内容预览:', aboutContent.substring(0, 300).replace(/\s+/g, ' '));
    
    await browser.close();
    console.log('\n完成!');
})();
