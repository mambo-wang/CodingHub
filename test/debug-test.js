const { chromium } = require('playwright');

(async () => {
    const browser = await chromium.launch({ headless: true });
    const page = await browser.newPage();
    
    // Enable console logging
    page.on('console', msg => console.log('Browser console:', msg.text()));
    page.on('pageerror', err => console.log('Page error:', err.message));
    
    console.log('Navigating to http://localhost:5173/tool/1...');
    await page.goto('http://localhost:5174/tool/1', { timeout: 30000 });
    
    console.log('\nWaiting for network idle...');
    await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {});
    await page.waitForTimeout(3000);
    
    console.log('\nCurrent URL:', page.url());
    
    // Get HTML content
    const html = await page.content();
    console.log('\nPage HTML length:', html.length);
    console.log('\nFirst 2000 chars of body:');
    const bodyMatch = html.match(/<body[^>]*>([\s\S]*)<\/body>/i);
    if (bodyMatch) {
        const bodyText = bodyMatch[1].replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim();
        console.log(bodyText.substring(0, 2000));
    }
    
    // Take screenshot
    await page.screenshot({ path: '/tmp/tool-detail-debug.png', fullPage: true });
    console.log('\nScreenshot saved to /tmp/tool-detail-debug.png');
    
    await browser.close();
})();
