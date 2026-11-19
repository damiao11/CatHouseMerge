/* Local design prototype only. No Android engine, save files, or network services. */
(() => {
  'use strict';
  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
  const stage = $('#canvas-stage');
  const board = $('#board-grid');
  const cells = $$('[data-cell]', board);
  const boardInitial = board.innerHTML;
  const top = $('[data-qb-section="top-bar"]');
  const leaf = (root, text) => $$('span', root).find(el => !el.children.length && el.textContent.trim() === text);
  const mainCoins = leaf(top, '440');
  const mainEnergy = leaf(top, '62');
  mainCoins.dataset.coins = '';
  mainCoins.classList.add('resource-value');
  mainEnergy.classList.add('resource-value');
  const energyPill = mainEnergy.closest('div');
  const coinsPill = mainCoins.closest('div');
  energyPill.classList.add('energy-pill');
  coinsPill.classList.add('coins-pill');
  energyPill.setAttribute('role','group');
  coinsPill.setAttribute('role','group');
  mainEnergy.parentElement.parentElement.classList.add('energy-value-stack');
  mainEnergy.parentElement.classList.add('energy-number-row');
  mainEnergy.parentElement.nextElementSibling.classList.add('energy-timer');
  mainCoins.parentElement.classList.add('coin-value-stack');
  const day = top.firstElementChild;
  day.classList.add('day-card');
  day.setAttribute('aria-label','第3天');
  day.children[1].classList.add('day-label');
  day.children[2].classList.add('day-number');
  day.children[1].textContent = '天';
  day.children[2].textContent = '3';
  const infoPanel = $('#tip-title').parentElement;
  infoPanel.id = 'item-info';
  infoPanel.classList.add('info-panel');
  $('#tip-sub').nextElementSibling?.remove();
  const infoHeading = document.createElement('div');
  infoHeading.className = 'info-heading';
  $('#tip-title').before(infoHeading);
  infoHeading.appendChild($('#tip-title'));
  infoHeading.insertAdjacentHTML('beforeend','<button type="button" class="item-info-button" data-action="item-info" aria-label="查看物品说明"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="2" y="2" width="20" height="20" rx="5" fill="#66BADD" stroke="#4196BB" stroke-width="2"/><circle cx="12" cy="7" r="1.6" fill="#fff8df"/><path d="M12 11v6" stroke="#fff8df" stroke-width="3" stroke-linecap="round"/></svg></button>');
  const actions = document.createElement('div');
  actions.className = 'info-actions';
  actions.innerHTML = '<span class="info-sale-label">出售</span><button type="button" data-action="sell" class="item-sell-button" aria-label="出售物品"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="10" fill="#F6B63F" stroke="#A96B36" stroke-width="1.8"/><path d="m8 6.5 4 5.5 4-5.5M8.5 12h7M8.5 15h7M12 12v6" fill="none" stroke="#8A5620" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/></svg><span id="sell-value">2</span></button>';
  infoPanel.appendChild(actions);
  const storageButton = $('[data-action="storage"]',stage);
  const storageCaption = leaf(storageButton,'第4天开放');
  const storageLockBadge = $('span',storageButton.firstElementChild);
  storageCaption.classList.add('storage-caption');

  const icons = {
    back:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m14.5 5-7 7 7 7"/></svg>',
    hammer:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m13 10-7.5 9.5a1.6 1.6 0 0 1-2.5-2L11 8"/><path d="m10 4 5-2 6 5-3 4-4-3-3 2-3-3z" fill="#fff0bf"/></svg>',
    check:'<svg viewBox="0 0 20 20" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="10" cy="10" r="8"/><path d="m6 10 2.5 2.5L14 7" fill="none"/></svg>',
    lamp:'<svg viewBox="0 0 36 42" fill="none" stroke="#a96b36" stroke-width="2.2" stroke-linejoin="round" stroke-linecap="round" aria-hidden="true"><path d="M18 3v5M12 8h12l-1 5H13z" fill="#e6c68a"/><path d="m12 13-5 7 3 15h16l3-15-5-7z" fill="#f6b63f"/><path d="M12 20h12l-2 11h-8z" fill="#fff8df"/><path d="M9 37h18M18 17v15"/></svg>'
  };
  const repair = document.createElement('section');
  repair.id = 'repair-page';
  repair.className = 'repair-page';
  repair.hidden = true;
  repair.setAttribute('aria-label', '门廊修缮');
  repair.innerHTML = `
    <header class="repair-top"><button type="button" class="icon-button" data-action="back-board" aria-label="返回合成">${icons.back}</button><div class="repair-heading"><h2>门廊</h2><p>第 3 天 · 给旧屋一点新意</p></div><div class="repair-wallet" aria-label="金币"><span class="coin-icon" aria-hidden="true">¥</span><span data-coins>440</span></div></header>
    <div class="repair-scene"><img id="porch-image" src="./assets/porch-before.webp" alt="待修缮门廊：木门、栏杆与空着的迎客灯挂钩" draggable="false"><div class="scene-switch"><span class="scene-label" id="scene-label">修缮前</span></div><div class="scene-caption"><span id="scene-caption">老门廊，等一盏迎客灯。</span><span aria-hidden="true"></span></div></div>
    <section class="repair-panel" aria-label="修缮任务"><div class="repair-panel-heading"><h3>门廊焕新计划</h3><span class="repair-fraction" id="repair-fraction">2 / 3</span></div><div class="repair-progress" role="progressbar" aria-label="门廊修缮进度" aria-valuemin="0" aria-valuemax="3" aria-valuenow="2"><span></span></div><div class="finished-tasks"><span class="finished-task">${icons.check}清理门廊</span><span class="finished-task">${icons.check}修补栏杆</span></div><div class="current-task"><div class="task-art">${icons.lamp}</div><div><h4 class="task-title" id="repair-task-title">挂上迎客灯</h4><p class="task-description" id="repair-task-description">暖暖的灯光，等每一只猫咪回家。</p></div></div><button type="button" class="primary-button" data-action="build" id="build-button">${icons.hammer}<span>修缮 · 300金币</span></button><button type="button" class="return-button" data-action="back-board" id="repair-return">先去合成，攒点材料</button></section>`;
  stage.appendChild(repair);
  const dialog = document.createElement('dialog');
  dialog.className = 'preview-dialog';
  dialog.setAttribute('aria-labelledby', 'dialog-title');
  dialog.innerHTML = '<div class="dialog-header"><h2 id="dialog-title"></h2><button type="button" data-action="close-dialog" aria-label="关闭">×</button></div><div class="dialog-body"></div>';
  document.body.appendChild(dialog);

  const state = {coins:440, energy:62, complete:false, comparison:false, scenario:'normal', selected:null, view:'board', orders:[false,false], storageUnlocked:false, stored:[], artMode:new URLSearchParams(location.search).get('icons') === 'sketch' ? 'sketch' : 'existing'};
  // Alias keys retain the imported demo layout; artwork identities use the real item catalog.
  const catalog = window.CatHouseAssetCatalog;
  const assetBases = {crate:'wood_4',plank:'wood_3',cloth:'fabric_2',screws:'metal_2',plant:'plant_3',hammer:'metal_1',stool:'wood_5',paint:'ceramic_4',brush:'ceramic_1',toolbox:'relic_4',lamp:'light_3'};
  const generatorArt = {bench:{name:'木工生成器',asset:'./assets/items/gen_wood_1.png'},paintpress:{name:'陶瓷生成器',asset:'./assets/items/gen_ceramic_1.png'}};
  function itemModel(itemOrKind, demoLevel = 1) {
    const kind = typeof itemOrKind === 'string' ? itemOrKind : itemOrKind.dataset.kind;
    const level = typeof itemOrKind === 'string' ? demoLevel : +itemOrKind.dataset.level;
    if(generatorArt[kind]) return {id:kind,chain:kind,level,name:generatorArt[kind].name,asset:generatorArt[kind].asset,generator:true,max:5};
    const base=catalog[assetBases[kind]];
    if(!base) throw new Error('Missing project artwork mapping: '+kind);
    const family=Object.values(catalog).filter(x=>x.chain===base.chain);
    const max=Math.max(...family.map(x=>x.level));
    const actualLevel=Math.min(max,base.level+level-1);
    const model=catalog[`${base.chain}_${actualLevel}`];
    return {...model,id:`${base.chain}_${actualLevel}`,max};
  }
  function itemPresentation(item) {
    return state.artMode==='existing' ? itemModel(item) : {name:item.dataset.label,level:+item.dataset.level};
  }
  function itemSaleValue(item) {return state.artMode==='existing' ? (itemModel(item).value || 2) : 2;}
  function itemDescription(item) {
    const model=itemModel(item);
    if(model.generator)return '点击产出材料，拖动可调整位置。';
    const specific={metal_1:'把松动的木件重新钉牢。',wood_3:'平整结实，适合修补旧屋。',wood_4:'收好零碎，也收好生活。',light_3:'一盏暖灯，照亮安静的角落。',relic_4:'停住的指针，藏着一段旧时光。'};
    const family={wood:'结实的木料，慢慢修好旧屋。',metal:'小小零件，让连接更牢靠。',fabric:'柔软的织物，给房间添点温暖。',light:'留一盏暖灯，等猫咪回家。',ceramic:'细细打磨，把日常过得精致。',plant:'一抹新绿，让窗边有了生机。',relic:'旧物里的故事，等你慢慢发现。'};
    return specific[model.id] || family[model.chain];
  }
  function decorateItem(item) {
    if(!item)return;
    if(!$('.sketch-art',item)) {
      const sketch=document.createElement('span');sketch.className='sketch-art';
      while(item.firstChild)sketch.appendChild(item.firstChild);
      item.appendChild(sketch);
      const image=document.createElement('img');image.className='game-item-art';image.alt='';image.draggable=false;item.appendChild(image);
      const demand=document.createElement('span');demand.className='asset-demand-mark';demand.setAttribute('aria-hidden','true');demand.innerHTML=icons.check;item.appendChild(demand);
      if(generatorArt[item.dataset.kind]) {
        const badge=document.createElement('span');badge.className='asset-generator-badge';badge.setAttribute('aria-hidden','true');badge.innerHTML='<i></i><i></i>';item.appendChild(badge);
      }
    }
    const model=itemModel(item),image=$('.game-item-art',item);
    if(image.getAttribute('src')!==model.asset)image.src=model.asset;
    item.dataset.assetId=model.id;
    item.title=state.artMode==='existing' ? `${model.name} · ${model.level}级` : `${item.dataset.label} · ${item.dataset.level}级`;
    item.classList.toggle('has-project-generator',!!model.generator);
    if(item.parentElement?.hasAttribute('data-cell')) {
      const spoken=itemPresentation(item);item.parentElement.setAttribute('aria-label',`${spoken.name}，${spoken.level}级${model.generator?'，点击产出材料':''}`);
    }
  }
  function setArtMode(mode,updateURL=true) {
    state.artMode=mode==='sketch'?'sketch':'existing';
    stage.dataset.artMode=state.artMode;
    $$('[data-art]').forEach(button=>button.setAttribute('aria-pressed',String(button.dataset.art===state.artMode)));
    $$('[data-item]',board).forEach(decorateItem);
    updateOrders();
    if(state.selected)select(state.selected);
    if(updateURL) {
      const url=new URL(location.href);url.searchParams.set('icons',state.artMode);
      try {history.replaceState(null,'',url.href);} catch (_) { /* file preview can still switch without changing its URL */ }
    }
  }
  const orderSpecs = [
    {requirements:[['plank',2,'木板'],['paint',1,'油漆桶']],reward:120},
    {requirements:[['cloth',2,'布料卷'],['plant',1,'花盆']],reward:90}
  ];
  const articles = $$('#order-scroll article');
  const orderUI = articles.map((article,index) => {
    const counts = $$('span', article).filter(el => !el.children.length && /^\d+\/\d+$/.test(el.textContent.trim()));
    const button = $('[data-action]', article);
    button.classList.add('order-button');
    button.nextElementSibling?.remove();
    article.classList.add('order-card');
    article.firstElementChild.classList.add('order-body');
    button.parentElement.classList.add('order-controls');
    article.appendChild($('[data-reward-strip]',article));
    const labels=orderSpecs[index].requirements.map(([kind,need,label])=>{
      const text=leaf(article,label),slot=text.previousElementSibling;
      const sketch=document.createElement('span');sketch.className='order-sketch-art';
      while(slot.firstChild)sketch.appendChild(slot.firstChild);
      slot.classList.add('order-asset-slot');slot.appendChild(sketch);
      const image=document.createElement('img');image.className='order-item-art';image.alt='';image.draggable=false;image.src=itemModel(kind).asset;slot.appendChild(image);
      return text;
    });
    return {article, counts, button, index, labels};
  });
  let toastTimer;
  function toast(message, duration = 2400) {
    const overlays = $('#app-overlays');
    overlays.replaceChildren();
    const el = document.createElement('div');
    el.className = 'preview-toast';
    el.textContent = message;
    overlays.appendChild(el);
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {el.classList.add('is-leaving');setTimeout(() => el.remove(),220);},duration);
  }
  function items(kind, level = 1) {const wanted=itemModel(kind,level);return $$('[data-item]',board).filter(el=>!el.dataset.generator&&itemModel(el).id===wanted.id);}
  function updateOrders() {
    $$('[data-item]',board).forEach(item=>{decorateItem(item);item.classList.remove('needed-for-order');});
    orderUI.forEach((ui,index) => {
      const spec = orderSpecs[index];
      const done = state.orders[index];
      let missing = '';
      spec.requirements.forEach(([kind,need,label],i) => {
        const stock=items(kind);
        const available=stock.length;
        const displayName=state.artMode==='existing'?itemModel(kind).name:label;
        ui.labels[i].textContent=displayName;
        if(!done)stock.slice(0,need).forEach(item=>item.classList.add('needed-for-order'));
        if(ui.counts[i]) ui.counts[i].textContent = `${done ? need : Math.min(available,need)}/${need}`;
        if(!done && available < need && !missing) missing = `差${need-available}${displayName}`;
      });
      ui.button.textContent = done ? '已交付' : missing || '交付';
      ui.button.disabled = done;
      ui.button.classList.toggle('order-ready',!done && !missing);
      ui.button.classList.toggle('order-done',done);
      ui.button.setAttribute('aria-label',done?'订单已交付':missing || `交付订单，获得${spec.reward}金币`);
    });
    if(state.selected) select(state.selected, false);
  }
  function deliver(index) {
    if(state.orders[index]) return;
    const spec = orderSpecs[index];
    const missing = spec.requirements.find(([kind,need]) => items(kind).length < need);
    if(missing) {toast(`还差${missing[1]-items(missing[0]).length}份${state.artMode==='existing'?itemModel(missing[0]).name:missing[2]}，试试点击生成器或合成材料。`);return;}
    spec.requirements.forEach(([kind,need]) => items(kind).slice(0,need).forEach(el => el.remove()));
    state.orders[index] = true;
    state.coins += spec.reward;
    updateOrders();renderResources();
    toast(`订单已交付，获得${spec.reward}金币。`,1200);
  }
  function renderResources() {
    $$('[data-coins]').forEach(el => el.textContent = state.coins);
    mainEnergy.textContent = state.energy;
    energyPill.setAttribute('aria-label',`体力${state.energy}，下一点恢复倒计时01分20秒`);
    coinsPill.setAttribute('aria-label',`金币${state.coins}`);
  }
  function renderRepair() {
    const showAfter = state.complete ? !state.comparison : false;
    const poor = !state.complete && state.coins < 300;
    repair.classList.toggle('is-poor',poor);
    repair.classList.toggle('is-complete',state.complete);
    $('#repair-fraction').textContent = state.complete ? '3 / 3' : '2 / 3';
    $('.repair-progress').setAttribute('aria-valuenow',state.complete ? '3' : '2');
    $('.repair-progress span').style.width = state.complete ? '100%' : '66.667%';
    $('#repair-task-title').textContent = state.complete ? '迎客灯亮起来了' : '挂上迎客灯';
    $('#repair-task-description').textContent = state.complete ? '门廊修好了，下一段故事等你开启。' : poor ? `还差${300-state.coins}金币，完成订单后再来。` : '暖暖的灯光，等每一只猫咪回家。';
    $('#build-button').classList.toggle('is-complete',state.complete);
    $('#build-button span').textContent = state.complete ? '返回合成' : poor ? '去合成赚金币' : '修缮 · 300金币';
    const comparisonButton = $('#repair-return');
    comparisonButton.textContent = state.complete ? (showAfter ? '重新看看修缮前' : '重新看看修缮后') : '返回合成，整理材料';
    comparisonButton.dataset.action = state.complete ? 'compare-scene' : 'back-board';
    if(state.complete) {
      comparisonButton.setAttribute('aria-pressed',String(state.comparison));
      comparisonButton.setAttribute('aria-controls','porch-image');
    } else {
      comparisonButton.removeAttribute('aria-pressed');
      comparisonButton.removeAttribute('aria-controls');
    }
    const img = $('#porch-image');
    // One comparison entry, in the footer; its label always names the other scene.
    const path = `./assets/porch-${showAfter ? 'after' : 'before'}.webp`;
    if(!img.getAttribute('src').endsWith(showAfter ? 'after.webp' : 'before.webp')) img.src = path;
    img.alt = showAfter ? '整间修好后的门廊，明亮的迎客灯与整洁的木地板' : '修缮前的门廊，斑驳木墙、旧地板与空灯钩';
    $('#scene-label').textContent = showAfter ? '门廊已修好' : '修缮前';
    $('#scene-caption').textContent = showAfter ? '灯亮了，欢迎回家。' : '老门廊，等一盏迎客灯。';
    renderResources();
  }
  function navigate(view, updateHash = true) {
    if(dialog.open) dialog.close();
    state.view = view === 'repair' ? 'repair' : 'board';
    repair.hidden = state.view !== 'repair';
    $$('[data-qb-section]').forEach(el => {
      if(el.dataset.qbSection !== 'app-overlays') el.hidden = state.view !== 'board';
    });
    $$('[data-view]').forEach(el => el.setAttribute('aria-pressed',String(el.dataset.view === state.view)));
    if(updateHash && location.hash !== `#${state.view}`) location.hash = state.view;
    renderRepair();
    requestAnimationFrame(layout);
  }
  function setScenario(value) {
    state.scenario = value;
    state.coins = value === 'poor' ? 120 : value === 'complete' ? 140 : 440;
    state.complete = value === 'complete';
    state.comparison = false;
    $('#preview-scenario').value = value;
    renderRepair();
  }
  function select(cell, announce = true) {
    if(!cell) return;
    state.selected = cell;
    $$('[data-cell]',board).forEach(el => {
      el.setAttribute('aria-selected',String(el === cell));
      el.tabIndex = el === cell ? 0 : -1;
      $('[data-item]',el)?.classList.toggle('item-selected',el === cell);
    });
    const item = $('[data-item]',cell);
    if(item)decorateItem(item);
    const presentation=item?itemPresentation(item):null;
    const generator = !!item?.hasAttribute('data-generator');
    $('#tip-title').textContent = item ? `${presentation.name} ${presentation.level}级` : '';
    $('#tip-sub').textContent = item ? itemDescription(item) : '';
    $('#tip-title').hidden = !item;
    $('#tip-sub').hidden = !item;
    infoHeading.hidden = !item;
    infoPanel.classList.toggle('has-selection',!!item);
    infoPanel.classList.toggle('is-generator',generator);
    const saleValue=item ? itemSaleValue(item) : 0;
    $('#sell-value').textContent=saleValue;
    $('[data-action="sell"]').setAttribute('aria-label',item ? `出售${presentation.name}，获得${saleValue}金币` : '出售物品');
    actions.hidden = !item || generator;
    $('[data-action="sell"]').disabled = !item || generator;
    cell.setAttribute('aria-label', item ? `${presentation.name}，${presentation.level}级${generator ? '，生成器' : ''}` : '空格子');
  }
  const templates = {};
  $$('[data-item]',board).forEach(el => {if(!templates[el.dataset.kind]) templates[el.dataset.kind] = el.cloneNode(true);});
  function prepareBoard() {
    const current = $$('[data-cell]',board);
    current.forEach((cell,index) => {
      cell.setAttribute('role','gridcell');
      cell.setAttribute('aria-rowindex',Math.floor(index/7)+1);
      cell.setAttribute('aria-colindex',index%7+1);
      const item = $('[data-item]',cell);
      if(item)decorateItem(item);
      const name=item?itemPresentation(item).name:'';
      cell.setAttribute('aria-label',item ? `${name}，${itemPresentation(item).level}级` : '空格子');
      if(item && ['bench','paintpress'].includes(item.dataset.kind)) {
        item.dataset.generator = item.dataset.kind === 'bench' ? 'plank' : 'paint';
        cell.setAttribute('aria-label',`${name}，点击产出材料`);
        cell.classList.add('generator-cell');
      }
    });
    select(current.find(el => el.hasAttribute('data-selected')) || current[0]);
  }
  function produce(item) {
    const empty = $$('[data-cell]',board).find(cell => !$('[data-item]',cell));
    if(!empty) return toast('棋盘满了，先合成或出售一些物品。');
    if(state.energy < 1) return toast('体力不足，请稍后再来。');
    const clone = templates[item.dataset.generator].cloneNode(true);
    clone.classList.remove('item-selected','is-dragging');
    clone.style.transform = '';
    empty.appendChild(clone);
    state.energy--;
    select(empty);updateOrders();renderResources();
    toast(`获得${itemPresentation(clone).name}，消耗1体力。`);
  }
  function resetDemo() {
    board.innerHTML = boardInitial;
    state.energy = 62;state.orders = [false,false];state.stored=[];state.storageUnlocked=false;renderStorageBadge();
    setScenario('normal');prepareBoard();updateOrders();layout();
    toast('演示已重置，未触及任何游戏存档。');
  }
  function build() {
    if(state.complete) return navigate('board');
    if(state.coins < 300) {navigate('board');return toast('完成猫咪订单赚取金币，再来修好门廊。');}
    state.coins -= 300;state.complete = true;state.comparison = false;
    $('#preview-scenario').value = 'complete';
    renderRepair();
    toast('门廊修好了！迎客灯亮起来了。',1200);
  }
  function layout() {
    const w = stage.clientWidth, h = stage.clientHeight;
    const compact = w <= 480 && h <= 700;
    const boardTop = compact ? 184 : 218;
    const actionHeight = compact ? 72 : 92;
    const bottom = compact ? 16 : 26;
    const maxHeight = h - bottom - actionHeight - (compact ? 12 : 16) - boardTop;
    const size = Math.max(18,Math.min((w-56)/7,(maxHeight-36)/9));
    stage.style.setProperty('--cell-size',`${size}px`);
    stage.style.setProperty('--item-scale',String(Math.min(1,size/44)));
    const panel = $('[data-qb-section="board-panel"]');
    panel.style.top = `${boardTop + Math.max(0,(maxHeight-(size*9+36))/2)}px`;
  }

  let drag = null;
  board.addEventListener('pointerdown',event => {
    if(event.button !== 0) return;
    const cell = event.target.closest('[data-cell]');
    if(!cell) return;
    const item = $('[data-item]',cell);
    select(cell);
    if(item) {
      drag = {item,cell,x:event.clientX,y:event.clientY,moved:false,pointer:event.pointerId};
      board.setPointerCapture(event.pointerId);
    }
  });
  board.addEventListener('pointermove',event => {
    if(!drag || event.pointerId !== drag.pointer) return;
    const dx=event.clientX-drag.x, dy=event.clientY-drag.y;
    if(!drag.moved && Math.hypot(dx,dy)>7) {drag.moved=true;drag.item.classList.add('is-dragging');}
    if(drag.moved) {
      drag.item.style.transform=`translate(${dx}px,${dy}px)`;
      const r=storageButton.getBoundingClientRect();
      const over=event.clientX>=r.left&&event.clientX<=r.right&&event.clientY>=r.top&&event.clientY<=r.bottom;
      if(over)storageButton.dataset.dropState=!state.storageUnlocked?'locked':state.stored.length>=6?'full':'ready';
      else delete storageButton.dataset.dropState;
    }
  });
  function releaseDrag(cancel = false, event = null) {
    if(!drag) return;
    const current=drag;drag=null;
    // Hit-test before restoring the dragged item: its return transition must not cover the drop target.
    const hit = !cancel && current.moved ? document.elementFromPoint(event.clientX,event.clientY) : null;
    const dropCell = hit?.closest('[data-cell]');
    const dropStorage = hit?.closest('[data-action="storage"]');
    delete storageButton.dataset.dropState;
    current.item.style.transform='';current.item.classList.remove('is-dragging');
    if(board.hasPointerCapture(current.pointer)) board.releasePointerCapture(current.pointer);
    if(cancel) return;
    if(!current.moved) {if(current.item.dataset.generator) produce(current.item);return;}
    if(dropStorage===storageButton)return storeItem(current.item);
    const cell=dropCell;
    if(!cell || !board.contains(cell) || cell===current.cell) return;
    const other=$('[data-item]',cell);
    if(!other) {cell.appendChild(current.item);select(cell);updateOrders();return;}
    if(other.dataset.generator || current.item.dataset.generator) return toast('生成器在本预览中只演示产出，不演示升级。');
    if(itemModel(other).id!==itemModel(current.item).id) return toast('只有同种、同级物品才能合成。');
    if(itemModel(other).level>=itemModel(other).max) return toast('已达到本预览的合成上限。');
    other.dataset.level=String(+other.dataset.level+1);
    current.item.remove();
    // Levels remain in the information panel, not on permanent item badges.
    other.classList.add('merge-pop');setTimeout(()=>other.classList.remove('merge-pop'),300);
    select(cell);updateOrders();
    toast(`合成成功：${itemPresentation(other).name}升到${itemPresentation(other).level}级。`);
  }
  board.addEventListener('pointerup',event=>releaseDrag(false,event));
  board.addEventListener('pointercancel',()=>releaseDrag(true));
  board.addEventListener('lostpointercapture',()=>releaseDrag(true));
  board.addEventListener('keydown',event=>{
    const cell=event.target.closest('[data-cell]');if(!cell)return;
    const list=$$('[data-cell]',board),index=list.indexOf(cell);
    const steps={ArrowLeft:-1,ArrowRight:1,ArrowUp:-7,ArrowDown:7};
    if(event.key in steps) {event.preventDefault();if((event.key==='ArrowLeft'&&index%7===0)||(event.key==='ArrowRight'&&index%7===6))return;const next=list[index+steps[event.key]];if(next){select(next);next.focus({preventScroll:true});}}
    if(event.key==='Enter'||event.key===' ') {event.preventDefault();const item=$('[data-generator]',cell);if(item)produce(item);else select(cell);}
  });
  function renderStorageBadge() {
    storageCaption.textContent=state.storageUnlocked ? `${state.stored.length}/6` : '第4天开放';
    storageLockBadge.hidden=state.storageUnlocked;
    storageButton.dataset.unlocked=String(state.storageUnlocked);
    storageButton.setAttribute('aria-label',state.storageUnlocked ? `仓库，已存${state.stored.length}件，共6格；可拖入物品存放` : '仓库，第4天开放');
  }
  function storeItem(item) {
    if(!state.storageUnlocked)return toast('仓库第4天开放，物品仍保留在棋盘。');
    if(state.stored.length>=6)return toast('仓库满了，先取回一些物品再存放。');
    const cell=item.parentElement;
    item.classList.remove('item-selected','is-dragging');item.style.transform='';
    state.stored.push(item);item.remove();
    select(cell);updateOrders();renderStorageBadge();
    toast(`已将${itemPresentation(item).name}放入仓库。`);
  }
  function openStorage() {
    if(!state.storageUnlocked) {
      openDialog('仓库 · 第4天开放','<p>仓库开放后，将棋盘上的物品拖到右下角仓库入口即可存放。</p><div class="storage-visual" aria-hidden="true"><span>待开放</span><span>待开放</span><span>待开放</span><span>待开放</span><span>待开放</span><span>待开放</span></div><button type="button" class="primary-button" data-action="close-dialog">继续整理棋盘</button><p class="help-detail">想先试拖拽？可在预览设置里开启“仓库开放预览”。不改变正式游戏进度。</p>');
      return;
    }
    const slots=Array.from({length:6},(_,index)=>{
      const item=state.stored[index];
      if(!item)return '<div class="storage-slot is-empty" aria-label="空仓库格">空位</div>';
      const m=itemModel(item),shown=itemPresentation(item);
      return `<button type="button" class="storage-slot" data-action="retrieve" data-storage-index="${index}" aria-label="取回${shown.name}"><img src="${m.asset}" alt="" draggable="false"><span>${shown.name}</span><small>${shown.level}级 · 取回</small></button>`;
    }).join('');
    openDialog(`仓库 · ${state.stored.length}/6`,'<p>存放时，把物品拖到棋盘右下角的仓库入口；点击这里的物品可取回。</p><div class="storage-grid">'+slots+'</div><p id="storage-message" class="storage-message" role="status"></p><button type="button" class="primary-button" data-action="close-dialog">返回棋盘</button>');
  }
  function retrieveItem(index) {
    const item=state.stored[index];if(!item)return;
    const empty=$$('[data-cell]',board).find(cell=>!$('[data-item]',cell));
    if(!empty){$('#storage-message').textContent='棋盘满了，先合成或出售一些物品，再来取回。';return;}
    state.stored.splice(index,1);empty.appendChild(item);
    dialog.close();select(empty);updateOrders();renderStorageBadge();
    toast(`已取回${itemPresentation(item).name}。`);
  }
  function openDialog(title,html) {
    $('#dialog-title',dialog).textContent=title;
    $('.dialog-body',dialog).innerHTML=html;
    dialog.showModal();
  }
  document.addEventListener('click',event=>{
    const art=event.target.closest('[data-art]');if(art)return setArtMode(art.dataset.art);
    const nav=event.target.closest('[data-view]');if(nav)return navigate(nav.dataset.view);
    const button=event.target.closest('[data-action]');if(!button || button.disabled)return;
    switch(button.dataset.action) {
      case 'repair': navigate('repair');break;
      case 'back-board': navigate('board');break;
      case 'build': build();break;
      case 'compare-scene': if(state.complete){state.comparison=!state.comparison;renderRepair();}break;
      case 'deliver': deliver(0);break;
      case 'deliver-next': deliver(1);break;
      case 'reset-demo': if(dialog.open)dialog.close();resetDemo();break;
      case 'sell': {
        const item=state.selected && $('[data-item]',state.selected);
        if(!item || item.dataset.generator)break;
        const price=itemSaleValue(item);item.remove();state.coins+=price;select(state.selected);updateOrders();renderResources();toast(`已出售物品，获得${price}金币。`);break;
      }
      case 'item-info': {
        const item=state.selected&&$('[data-item]',state.selected);if(!item)break;
        const shown=itemPresentation(item);
        openDialog(`${shown.name} ${shown.level}级`,`<p>${itemDescription(item)}</p><p class="help-detail">同种同级物品可以合成。仓库开放后，将物品拖到仓库入口存放；订单只认棋盘上的物品。</p><button type="button" class="primary-button" data-action="close-dialog">知道了</button>`);break;
      }
      case 'storage': openStorage();break;
      case 'retrieve': retrieveItem(+button.dataset.storageIndex);break;
      case 'settings': openDialog('预览设置',`<div class="art-options"><button type="button" data-art="existing" aria-pressed="${state.artMode==='existing'}">现有游戏素材</button><button type="button" data-art="sketch" aria-pressed="${state.artMode==='sketch'}">原版示意</button></div><label class="setting-row">音效开关示意<input type="checkbox" checked aria-label="音效开关示意"></label><label class="setting-row">音乐开关示意<input type="checkbox" checked aria-label="音乐开关示意"></label><label class="setting-row">仓库开放预览<input type="checkbox" id="preview-storage" aria-label="仓库开放预览" ${state.storageUnlocked?'checked':''}></label><p>修缮状态：切换金币不足和已完成效果。</p><select class="dialog-select" id="dialog-scenario" aria-label="修缮状态"><option value="normal">正常 · 440金币</option><option value="poor">金币不足 · 120金币</option><option value="complete">房间已完成</option></select><button type="button" class="primary-button" data-action="reset-demo">重置本地演示</button><p class="help-detail">开关仅展示界面状态，未接入音频。所有操作只在当前预览中生效，不读取或修改游戏存档。</p>`);$('#dialog-scenario').value=state.scenario;break;
      case 'close-dialog': dialog.close();break;
    }
  });
  document.addEventListener('change',event=>{
    if(event.target.id==='preview-scenario'||event.target.id==='dialog-scenario')setScenario(event.target.value);
    if(event.target.id==='preview-storage'){state.storageUnlocked=event.target.checked;renderStorageBadge();}
  });
  dialog.addEventListener('click',event=>{if(event.target===dialog){const r=dialog.getBoundingClientRect();if(event.clientX<r.left||event.clientX>r.right||event.clientY<r.top||event.clientY>r.bottom)dialog.close();}});
  addEventListener('hashchange',()=>navigate(location.hash==='#repair'?'repair':'board',false));
  new ResizeObserver(layout).observe(stage);
  prepareBoard();setArtMode(state.artMode,false);renderStorageBadge();renderResources();navigate(location.hash==='#repair'?'repair':'board',false);layout();
})();
