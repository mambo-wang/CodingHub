const { chromium } = require('playwright');

const BASE = 'http://localhost:5173';
const S = 'D:\\repos\\CodingHub';

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();

  try {
    // 1. Home page
    console.log('1. Home page...');
    await page.goto(BASE, { waitUntil: 'networkidle', timeout: 15000 }).catch(() => {});
    await page.waitForTimeout(2000);
    await page.screenshot({ path: S + '/screenshot-01-home.png', fullPage: true });

    // 2. Forum page
    console.log('2. Forum page...');
    await page.goto(BASE + '/forum', { waitUntil: 'networkidle', timeout: 15000 }).catch(() => {});
    await page.waitForTimeout(2000);
    await page.screenshot({ path: S + '/screenshot-02-forum.png', fullPage: true });
    console.log('   Title:', await page.title());

    // 3. Check if there are any posts
    const postLinks = page.locator('a[href*="/forum/posts/"]');
    const postCount = await postLinks.count();
    console.log('   Post links found:', postCount);

    // 4. Login page
    console.log('3. Login page...');
    await page.goto(BASE + '/login', { waitUntil: 'networkidle', timeout: 15000 }).catch(() => {});
    await page.waitForTimeout(1000);
    await page.screenshot({ path: S + '/screenshot-03-login.png', fullPage: false });

    // Get page content as text
    const content = await page.locator('body').innerText();
    console.log('   Page content (first 300 chars):', content.substring(0, 300));

    // 5. Post detail (if exists)
    if (postCount > 0) {
      console.log('4. Post detail...');
      const href = await postLinks.first().getAttribute('href');
      await page.goto(BASE + href, { waitUntil: 'networkidle', timeout: 15000 }).catch(() => {});
      await page.waitForTimeout(2000);
      await page.screenshot({ path: S + '/screenshot-04-post-detail.png', fullPage: true });
    }

    // 6. My Tools / Tools page
    console.log('5. Tools page...');
    await page.goto(BASE + '/tools', { waitUntil: 'networkidle', timeout: 15000 }).catch(() => {});
    await page.waitForTimeout(2000);
    await page.screenshot({ path: S + '/screenshot-05-tools.png', fullPage: true });

    console.log('\nAll screenshots saved to', S);
  } catch (err) {
    console.error('Error:', err.message);
    try { await page.screenshot({ path: S + '/screenshot-error.png' }); } catch(e) {}
  } finally {
    await browser.close();
  }
})();
