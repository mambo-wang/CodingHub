const { chromium } = require('playwright');

(async () => {
    const browser = await chromium.launch({ headless: true });
    const page = await browser.newPage({ viewport: { width: 1400, height: 900 } });
    
    // Collect console messages
    const logs = [];
    page.on('console', msg => {
        if (msg.type() === 'error') {
            logs.push(`ERROR: ${msg.text()}`);
        }
    });
    
    console.log('=== 快速开始页面调试 ===');
    await page.goto('http://localhost:5174/quickstart', { timeout: 30000 });
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    
    // Check for any visible content
    const bodyHTML = await page.evaluate(() => document.body.innerHTML);
    console.log('Body HTML length:', bodyHTML.length);
    
    // Check main container
    const mainContent = await page.locator('.page-container').count();
    console.log('page-container elements:', mainContent);
    
    const quickstartPage = await page.locator('.quickstart-page').count();
    console.log('.quickstart-page elements:', quickstartPage);
    
    if (logs.length > 0) {
        console.log('Console errors:', logs);
    } else {
        console.log('No console errors');
    }
    
    console.log('\n=== 关于页面调试 ===');
    await page.goto('http://localhost:5174/about', { timeout: 30000 });
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);
    
    const aboutBody = await page.evaluate(() => document.body.innerHTML);
    console.log('About Body HTML length:', aboutBody.length);
    
    const aboutPage = await page.locator('.about-page').count();
    console.log('.about-page elements:', aboutPage);
    
    const pageContainer = await page.locator('.page-container').count();
    console.log('.page-container elements:', pageContainer);
    
    if (logs.length > 0) {
        console.log('Errors:', logs);
    }
    
    // Take screenshot
    await page.screenshot({ path: '/tmp/debug-about.png', fullPage: true });
    console.log('\n截图保存到 /tmp/debug-about.png');
    
    await browser.close();
})();
