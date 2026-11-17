const { chromium } = require('playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
(async () => {
 const executablePath = ['C:/Program Files/Google/Chrome/Application/chrome.exe','C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe'].find(fs.existsSync);
 const browser = await chromium.launch({headless:true,executablePath});
 const errors=[], results=[];
 const base=process.env.PREVIEW_URL||'http://127.0.0.1:8765/';
 const out=path.join(__dirname,'previews');fs.mkdirSync(out,{recursive:true});
 async function capture(name,width,height,hash='board',scenario='normal',artMode='existing') {
  const page=await browser.newPage({viewport:{width,height},deviceScaleFactor:1});
  page.on('pageerror',e=>errors.push(`${name}: ${e.message}`));
  await page.goto(`${base}${artMode==='sketch'?'?icons=sketch':''}#${hash}`);await page.evaluate(()=>document.fonts.ready);
  if(scenario!=='normal') {
   await page.evaluate(value=>{const s=document.querySelector('#preview-scenario');s.value=value;s.dispatchEvent(new Event('change',{bubbles:true}));},scenario);
  }
  await page.waitForTimeout(200);
  const layout=await page.evaluate(()=>{
   const rect=el=>{const r=el.getBoundingClientRect();return {x:r.x,y:r.y,width:r.width,height:r.height,bottom:r.bottom,right:r.right};};
   return {stage:rect(document.querySelector('#canvas-stage')),cells:document.querySelectorAll('[data-cell]').length,cell:rect(document.querySelector('[data-cell]')),scrollWidth:document.documentElement.scrollWidth,scrollHeight:document.documentElement.scrollHeight,action:rect(document.querySelector('[data-qb-section="action-bar"]')),repairPanel:rect(document.querySelector('.repair-panel')),scene:rect(document.querySelector('.repair-scene')),imageLoaded:document.querySelector('#porch-image').complete&&document.querySelector('#porch-image').naturalWidth>0};
  });
  assert.equal(layout.cells,63);
  if(hash==='board')assert.ok(Math.abs(layout.cell.width-layout.cell.height)<.1,'cells must be square');
  if(width<=480) {assert.ok(layout.scrollWidth<=width,`${name} horizontal overflow`);assert.ok(layout.scrollHeight<=height,`${name} vertical overflow`);if(hash==='board')assert.ok(layout.action.bottom<=height,`${name} bottom clipped`);else assert.ok(layout.repairPanel.bottom<=height&&layout.scene.height>=120,`${name} repair clipped`);}
  assert.ok(layout.imageLoaded,`${name} missing room artwork`);
  await page.screenshot({path:path.join(out,`${name}.png`)});
  results.push({name,...layout});
  await page.close();
 }
 try {
  await capture('board-desktop',1280,940);
  await capture('board-sketch',360,780,'board','normal','sketch');
  await capture('repair-desktop',1280,940,'repair');
  await capture('board-mobile',360,780);
  await capture('repair-mobile',360,780,'repair');
  await capture('repair-insufficient',360,780,'repair','poor');
  await capture('repair-completed',360,780,'repair','complete');
  await capture('board-compact',320,640);
  await capture('repair-compact',320,640,'repair');
  await capture('board-small',320,568);
  await capture('repair-small',320,568,'repair');
  const p=await browser.newPage({viewport:{width:360,height:780}});
  p.on('pageerror',e=>errors.push(`interaction: ${e.message}`));
  await p.goto(base);await p.evaluate(()=>document.fonts.ready);
  const checks=[];
  async function check(name,fn){await fn();checks.push(name);}
  const coin=()=>p.locator('[data-coins]').first().textContent();
  async function assertRaisedToast() {
   const placement=await p.locator('.preview-toast').evaluate(el=>{
    const r=el.getBoundingClientRect(),s=document.querySelector('#canvas-stage').getBoundingClientRect();
    return {center:(r.y+r.height/2-s.y)/s.height,top:r.y-s.y,bottom:r.bottom-s.y,stageHeight:s.height};
   });
   assert.ok(placement.center>.35&&placement.center<.55,`toast should sit near center, not at bottom: ${JSON.stringify(placement)}`);
  }

  await check('header uses icons and numbers, with a day label above the numeral',async()=>{
   const header=p.locator('[data-qb-section="top-bar"]');
   assert.doesNotMatch(await header.innerText(),/体力|金币/);
   assert.equal(await p.locator('.day-label').textContent(),'天');
   assert.equal(await p.locator('.day-number').textContent(),'3');
   const label=await p.locator('.day-label').boundingBox(),number=await p.locator('.day-number').boundingBox();
   assert.ok(label.y+label.height<=number.y+.5);
   const rewards=await p.locator('[data-reward-strip]').allInnerTexts();
   assert.deepEqual(rewards.map(text=>text.replace(/\s/g,'')),['¥120','¥90']);
  });
  await check('cream order cards put coins in their lower-right corner without status hints',async()=>{
   assert.doesNotMatch(await p.locator('#order-scroll').innerText(),/材料已备齐|点击生成器补货|奖励已领取/);
   const placements=await p.locator('[data-reward-strip]').evaluateAll(strips=>strips.map(el=>{
    const r=el.getBoundingClientRect(),a=el.closest('article').getBoundingClientRect(),css=getComputedStyle(el);
    return {right:a.right-r.right,bottom:a.bottom-r.bottom,width:r.width,cardWidth:a.width,bg:css.backgroundColor};
   }));
   for(const r of placements){assert.ok(r.right<20&&r.bottom<20&&r.width<r.cardWidth/2);assert.equal(r.bg,'rgba(0, 0, 0, 0)');}
   assert.equal(await p.locator('[data-action="deliver"]').evaluate(el=>el.classList.contains('order-ready')),true);
   assert.equal(await p.locator('.day-number').evaluate(el=>getComputedStyle(el).color),'rgb(98, 65, 37)');
  });
  await check('order completion checks use a normal non-mirrored SVG tick',async()=>{
   const ticks=p.locator('#order-scroll svg.completion-tick');
   assert.equal(await ticks.count(),2);
   assert.deepEqual(await ticks.locator('path').evaluateAll(paths=>paths.map(el=>el.getAttribute('d'))),['M2 6 5 9 10 3','M2 6 5 9 10 3']);
   assert.equal(await p.locator('#order-scroll .-rotate-45').count(),0);
   await p.locator('#order-scroll article').first().screenshot({path:path.join(out,'order-check-fixed.png')});
  });
  await check('order contents shift together while enlarged detail text stays clear of selling',async()=>{
   const order=await p.locator('#order-scroll article').first().evaluate(el=>{
    const card=el.getBoundingClientRect(),avatar=el.querySelector(':scope > svg').getBoundingClientRect();
    const body=el.querySelector('.order-body'),materials=body.firstElementChild.getBoundingClientRect();
    const controls=el.querySelector('.order-controls').getBoundingClientRect();
    return {avatarLeft:avatar.x-card.x,paddingLeft:parseFloat(getComputedStyle(body).paddingLeft),materialRight:materials.right,controlsLeft:controls.x,controlsRight:card.right-controls.right};
   });
   assert.ok(order.avatarLeft>0&&order.paddingLeft===64);
   assert.ok(order.materialRight<order.controlsLeft);
   assert.ok(order.controlsRight<15);
   for(const [width,height,minFont] of [[360,780,13],[320,640,12],[320,568,12]]){
    await p.setViewportSize({width,height});
    await p.waitForTimeout(100);
    const info=await p.locator('#tip-sub').evaluate(el=>{
     const r=el.getBoundingClientRect(),sell=document.querySelector('[data-action="sell"]').getBoundingClientRect(),panel=document.querySelector('#item-info').getBoundingClientRect();
     return {font:parseFloat(getComputedStyle(el).fontSize),right:r.right,bottom:r.bottom,sellLeft:sell.x,panelBottom:panel.bottom};
    });
    assert.ok(info.font>=minFont);
    assert.ok(info.right<=info.sellLeft);
    assert.ok(info.bottom<=info.panelBottom-4);
   }
   await p.setViewportSize({width:360,height:780});
  });
  await check('empty selection clears the information panel without shifting the board',async()=>{
   const before=await p.locator('[data-qb-section="board-panel"]').boundingBox();
   await p.locator('[data-cell]').first().click();
   assert.equal(await p.locator('#tip-title').textContent(),'');
   assert.equal(await p.locator('#tip-sub').textContent(),'');
   assert.equal((await p.locator('#item-info').innerText()).trim(),'');
   assert.equal(await p.locator('.info-actions').isVisible(),false);
   assert.deepEqual(await p.locator('[data-qb-section="board-panel"]').boundingBox(),before);
   await p.screenshot({path:path.join(out,'board-empty-selection.png')});
  });
  await check('select item updates info and unique highlight',async()=>{
   await p.locator('[data-kind="hammer"]').first().click();
   assert.match(await p.locator('#tip-title').textContent(),/钉子/);
   assert.equal(await p.locator('[data-cell][aria-selected="true"]').count(),1);
  });
  await check('project artwork loads for every board item and order requirement',async()=>{
   await p.waitForFunction(()=>[...document.querySelectorAll('.game-item-art,.order-item-art')].every(img=>img.complete&&img.naturalWidth>0));
   assert.equal(await p.locator('.game-item-art').count(),await p.locator('[data-item]').count());
   assert.equal(await p.locator('.order-item-art').count(),4);
   assert.equal(await p.locator('#canvas-stage').getAttribute('data-art-mode'),'existing');
   assert.match(await p.locator('#tip-title').textContent(),/钉子/);
  });
  await check('artwork comparison preserves inventory and selection',async()=>{
   const count=await p.locator('[data-item]').count();
   await p.locator('[data-action="settings"]').click();
   await p.locator('dialog [data-art="sketch"]').click();
   assert.equal(await p.locator('#canvas-stage').getAttribute('data-art-mode'),'sketch');
   assert.match(await p.locator('#tip-title').textContent(),/锤子/);
   await p.locator('dialog [data-art="existing"]').click();
   assert.equal(await p.locator('[data-item]').count(),count);
   assert.match(await p.locator('#tip-title').textContent(),/钉子/);
   await p.keyboard.press('Escape');
  });
  await check('description and sell control match the selected item with no store button',async()=>{
   assert.equal(await p.locator('[data-action="store"]').count(),0);
   await p.locator('[data-kind="plank"]').first().click();
   assert.match(await p.locator('#tip-sub').textContent(),/平整结实/);
   assert.equal(await p.locator('#sell-value').textContent(),'12');
   await p.locator('[data-action="item-info"]').click();
   assert.match(await p.locator('#dialog-title').textContent(),/木板/);
   await p.keyboard.press('Escape');
   const before=+(await coin());
   await p.locator('[data-action="sell"]').click();
   assert.equal(+(await coin()),before+12);
   assert.equal((await p.locator('#item-info').innerText()).trim(),'');
   await reset();
  });
  await check('generator produces one item and consumes one energy',async()=>{
   const count=await p.locator('[data-item]').count();
   await p.locator('[data-kind="bench"]').click();
   assert.equal(await p.locator('[data-item]').count(),count+1);
   assert.equal((await p.locator('.resource-value').first().textContent()).trim(),'61');
  });
  await check('delivery uses real board items and rewards once',async()=>{
   const before=+(await coin());
   const deliveredAt=Date.now();
   await p.locator('[data-action="deliver"]').click();
   assert.equal(+(await coin()),before+120);
   assert.equal(await p.locator('[data-action="deliver"]').isDisabled(),true);
   assert.match(await p.locator('.preview-toast').textContent(),/获得120金币/);
   await assertRaisedToast();
   await p.screenshot({path:path.join(out,'order-reward-toast.png')});
   await p.locator('.preview-toast').waitFor({state:'detached',timeout:1800});
   assert.ok(Date.now()-deliveredAt>=1000&&Date.now()-deliveredAt<2200,'order reward should hold for about 1.2 seconds, then fade');
  });
  await check('locked storage dialog opens and Escape closes',async()=>{
   await p.locator('[data-action="storage"]').click();
   assert.equal(await p.locator('dialog').evaluate(el=>el.open),true);
   await p.keyboard.press('Escape');
   assert.equal(await p.locator('dialog').evaluate(el=>el.open),false);
  });
  await check('repair navigation has no redundant top-right comparison button',async()=>{
   const before=await coin();
   await p.locator('[data-action="repair"]').click();
   assert.equal(await p.locator('#repair-page').isVisible(),true);
   assert.equal(await p.locator('.scene-switch button').count(),0);
   assert.equal(await p.locator('#scene-label').textContent(),'修缮前');
   assert.match(await p.locator('#porch-image').getAttribute('src'),/before/);
   assert.equal(await coin(),before);
  });
  await check('repair spends exactly once and reaches 3 of 3',async()=>{
   const before=+(await coin());
   const builtAt=Date.now();
   await p.locator('#build-button').click();
   assert.equal(+(await coin()),before-300);
   assert.equal(await p.locator('#repair-fraction').textContent(),'3 / 3');
   assert.match(await p.locator('.preview-toast').textContent(),/门廊修好了/);
   await assertRaisedToast();
   await p.screenshot({path:path.join(out,'repair-success-toast.png')});
   assert.equal(await p.locator('#repair-return').textContent(),'重新看看修缮前');
   await p.locator('.preview-toast').waitFor({state:'detached',timeout:1800});
   assert.ok(Date.now()-builtAt>=1000&&Date.now()-builtAt<2200,'build success should hold for about 1.2 seconds, then fade');
   await p.locator('#repair-return').click();
   assert.match(await p.locator('#porch-image').getAttribute('src'),/before/);
   assert.equal(await p.locator('#scene-label').textContent(),'修缮前');
   assert.equal(await p.locator('#repair-return').textContent(),'重新看看修缮后');
   assert.equal(await p.locator('[data-action="compare-scene"]').count(),1);
   await p.screenshot({path:path.join(out,'repair-completed-before.png')});
   await p.locator('#repair-return').click();
   assert.match(await p.locator('#porch-image').getAttribute('src'),/after/);
   assert.equal(await p.locator('#repair-return').textContent(),'重新看看修缮前');
   assert.equal(+(await coin()),before-300);

   await p.locator('#build-button').click();
   assert.equal(+(await coin()),before-300);
   assert.equal(await p.locator('#repair-page').isVisible(),false);
  });
  await check('insufficient funds explain deficit and return to board',async()=>{
   await p.evaluate(()=>{const s=document.querySelector('#preview-scenario');s.value='poor';s.dispatchEvent(new Event('change',{bubbles:true}));});
   await p.locator('[data-action="repair"]').click();
   assert.match(await p.locator('#repair-task-description').textContent(),/180/);
   await p.locator('#build-button').click();
   assert.equal(await coin(),'120');
   assert.equal(await p.locator('#repair-page').isVisible(),false);
  });
  async function reset(){await p.locator('[data-action="settings"]').click();await p.locator('dialog [data-action="reset-demo"]').click();}
  async function drag(from,to){
   // Unlike locator.click(), raw mouse coordinates do not auto-wait for a rejected drop's return transition.
   await from.evaluate(async el=>{await Promise.all(el.getAnimations().map(a=>a.finished.catch(()=>{})));});
   const a=await from.boundingBox(),b=await to.boundingBox();
   await p.mouse.move(a.x+a.width/2,a.y+a.height/2);await p.mouse.down();
   await p.mouse.move(b.x+b.width/2,b.y+b.height/2,{steps:12});await p.mouse.up();
  }
  await check('reset restores demo without touching save state',async()=>{await reset();assert.equal(await coin(),'440');assert.equal(await p.locator('[data-action="deliver"]').isDisabled(),false);});
  await check('drag to empty cell moves one item',async()=>{
   const target=p.locator('[data-cell]').first();
   await drag(p.locator('[data-kind="hammer"]').first(),target);
   assert.equal(await target.locator('[data-kind="hammer"]').count(),1);
  });
  await check('matching items merge and selection follows target',async()=>{
   await reset();
   const first=p.locator('[data-kind="plank"]').nth(0),second=p.locator('[data-kind="plank"]').nth(1);
   const count=await p.locator('[data-item]').count();
   await drag(first,second);
   assert.equal(await p.locator('[data-item]').count(),count-1);
   assert.equal(await p.locator('[data-kind="plank"][data-level="2"]').count(),1);
   assert.match(await p.locator('[data-kind="plank"][data-level="2"] .game-item-art').getAttribute('src'),/ic_wood_4\.png$/);
   assert.match(await p.locator('#tip-title').textContent(),/4级/);
  });
  await check('different items do not merge',async()=>{
   const count=await p.locator('[data-item]').count();
   await drag(p.locator('[data-kind="hammer"]').first(),p.locator('[data-kind="crate"]').first());
   assert.equal(await p.locator('[data-item]').count(),count);
  });
  await check('keyboard moves grid selection',async()=>{
   const cell=p.locator('[data-cell][aria-selected="true"]');
   const before=await cell.getAttribute('aria-colindex');await cell.focus();await p.keyboard.press('ArrowRight');
   assert.equal(+(await p.locator('[data-cell][aria-selected="true"]').getAttribute('aria-colindex')),+before+1);
  });
  async function unlockStorage(){await p.locator('[data-action="settings"]').click();await p.locator('#preview-storage').check();await p.keyboard.press('Escape');}
  await check('dragging into locked storage leaves the item on the board',async()=>{
   await reset();const count=await p.locator('[data-item]').count();
   await drag(p.locator('[data-kind="plank"]').first(),p.locator('[data-action="storage"]'));
   assert.equal(await p.locator('[data-item]').count(),count);
   assert.match(await p.locator('.preview-toast').textContent(),/第4天开放/);
   assert.equal(await p.locator('.storage-caption').textContent(),'第4天开放');
   await p.waitForTimeout(1400);
   assert.equal(await p.locator('.preview-toast').isVisible(),true,'other informational toasts must keep their original longer duration');
  });
  await check('opened storage accepts dragged items and returns them intact',async()=>{
   await unlockStorage();const count=await p.locator('[data-item]').count(),coins=await coin();
   await drag(p.locator('[data-kind="plank"]').first(),p.locator('[data-action="storage"]'));
   assert.equal(await p.locator('[data-item]').count(),count-1);
   assert.equal(await p.locator('.storage-caption').textContent(),'1/6');
   assert.equal((await p.locator('#item-info').innerText()).trim(),'');
   await p.locator('[data-action="storage"]').click();
   assert.equal(await p.locator('.storage-slot img').count(),1);
   assert.match(await p.locator('.storage-slot img').getAttribute('src'),/ic_wood_3\.png$/);
   await p.screenshot({path:path.join(out,'warehouse-stored.png')});
   await p.locator('[data-action="retrieve"]').click();
   assert.equal(await p.locator('[data-item]').count(),count);
   assert.equal(await p.locator('.storage-caption').textContent(),'0/6');
   assert.equal(await coin(),coins);
   assert.match(await p.locator('#tip-title').textContent(),/木板 3级/);
  });
  await check('stored materials cannot be used to deliver an order',async()=>{
   await reset();await unlockStorage();
   await drag(p.locator('[data-kind="paint"]').first(),p.locator('[data-action="storage"]'));
   await drag(p.locator('[data-kind="paint"]').first(),p.locator('[data-action="storage"]'));
   assert.equal(await p.locator('[data-action="deliver"]').evaluate(el=>el.classList.contains('order-ready')),false);
   const coins=await coin();await p.locator('[data-action="deliver"]').click();
   assert.equal(await coin(),coins);
   assert.equal(await p.locator('.storage-caption').textContent(),'2/6');
  });
  await check('full storage rejects another item without losing it',async()=>{
   await reset();await unlockStorage();
   for(let i=0;i<6;i++)await drag(p.locator('[data-item]:not([data-generator])').first(),p.locator('[data-action="storage"]'));
   const count=await p.locator('[data-item]').count();
   await drag(p.locator('[data-item]:not([data-generator])').first(),p.locator('[data-action="storage"]'));
   assert.equal(await p.locator('[data-item]').count(),count);
   assert.equal(await p.locator('.storage-caption').textContent(),'6/6');
   assert.match(await p.locator('.preview-toast').textContent(),/仓库满了/);
   await reset();
  });
  await check('reward toast is raised on compact screens too',async()=>{
   await p.setViewportSize({width:320,height:568});
   await reset();
   await p.locator('[data-action="deliver"]').click();
   await assertRaisedToast();
  });
  await check('local HTML entry works without a web server',async()=>{
   await p.goto(require('node:url').pathToFileURL(path.join(__dirname,'index.html')).href+'#repair');
   await p.evaluate(()=>document.fonts.ready);
   assert.equal(await p.locator('#repair-page').isVisible(),true);
   assert.equal(await p.locator('#porch-image').evaluate(el=>el.complete&&el.naturalWidth>0),true);
   assert.equal(await p.evaluate(()=>document.fonts.check('14px CatHouse')),true);
  });
  await p.close();
  assert.deepEqual(errors,[],'browser errors');
  fs.writeFileSync(path.join(out,'verification.json'),JSON.stringify({errors,results,interactionChecks:checks},null,2));
  console.log(JSON.stringify({viewportChecks:results.length,interactionChecks:checks,errors},null,2));
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
