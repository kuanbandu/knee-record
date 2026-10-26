/**
 * 临时冒烟测试：用桩替代 wx API，在 Node 里跑通记录型小程序的页面流程。
 * 仅用于开发期自检，不属于小程序产物。
 *
 * 覆盖：记一次锻炼 → 保存 → 记录页展示与趋势图 → 删除 → 个人资料 → 设置清除。
 */

const storage = {};
const toasts = [];
const navigations = [];

function clone(v) {
  return v === undefined ? undefined : JSON.parse(JSON.stringify(v));
}

global.wx = {
  getStorageSync: (k) => (Object.prototype.hasOwnProperty.call(storage, k) ? storage[k] : ''),
  setStorageSync: (k, v) => { storage[k] = clone(v); },
  removeStorageSync: (k) => { delete storage[k]; },
  getSystemInfoSync: () => ({ pixelRatio: 2 }),
  showToast: (o) => { toasts.push(o.title); },
  showModal: (o) => { toasts.push(`[modal] ${o.title}`); if (o.success) o.success({ confirm: true }); },
  switchTab: (o) => { navigations.push(`switchTab ${o.url}`); },
  navigateTo: (o) => { navigations.push(`navigateTo ${o.url}`); },
  navigateBack: () => { navigations.push('navigateBack'); },
  createSelectorQuery: () => {
    const q = {
      in() { return q; },
      select() { return q; },
      fields() { return q; },
      exec(cb) { cb([null]); },
    };
    return q;
  },
};

const pageDefs = [];
global.Page = (def) => { pageDefs.push(def); return def; };

function makePage(def) {
  const page = Object.assign({}, def);
  page.data = clone(def.data) || {};
  page.setData = function (patch, cb) {
    Object.assign(this.data, patch);
    if (cb) cb();
  };
  return page;
}

const base = require('path').resolve(__dirname, '..', 'miniprogram');
const loadPage = (rel) => {
  require(base + rel);
  return makePage(pageDefs[pageDefs.length - 1]);
};

// ------------------------------------------------------------------ 录入页
const entry = loadPage('/pages/entry/entry.js');

console.log('空动作时保存 →', (entry.onSave(), toasts.pop()));

entry.onNameInput({ currentTarget: { dataset: { index: 0 } }, detail: { value: '靠墙静蹲' } });
entry.onSetsInput({ currentTarget: { dataset: { index: 0 } }, detail: { value: '3' } });
entry.onAddExercise();
entry.onNameInput({ currentTarget: { dataset: { index: 1 } }, detail: { value: '直腿抬高' } });
entry.onSetsInput({ currentTarget: { dataset: { index: 1 } }, detail: { value: '2' } });
entry.onFeelingChange({ detail: { value: 3 } });
entry.onDurationInput({ detail: { value: '25' } });
entry.onNoteInput({ detail: { value: '今天在健身房' } });
entry.onSave();

const saved = (storage['knee:records'] || [])[0];
console.log('记录写入 =', saved && saved.exercises.map((e) => `${e.name} ${e.sets}组`).join(' / '),
  '｜身体感受', saved && saved.feelingScore, '｜时长', saved && saved.durationMin, '分钟');
console.log('保存后跳转 →', navigations.pop(), '｜toast =', toasts.pop());

// 时长非法
const entry2 = loadPage('/pages/entry/entry.js');
entry2.onNameInput({ currentTarget: { dataset: { index: 0 } }, detail: { value: '踝泵' } });
entry2.onDurationInput({ detail: { value: '999' } });
entry2.onSave();
console.log('时长 999 分钟 →', toasts.pop());

// ------------------------------------------------------------------ 记录页
const recordsPage = loadPage('/pages/records/records.js');
recordsPage.onShow();
console.log('记录页 =', recordsPage.data.records.length, '条｜累计', recordsPage.data.totalMin,
  '分钟｜图表', recordsPage.data.chartReady);
console.log('展示文本 =', recordsPage.data.records[0].exerciseTexts.join(' / '),
  '｜', recordsPage.data.records[0].durationText, '｜', recordsPage.data.records[0].dateText);

recordsPage.onGoEntry();
console.log('录入入口 →', navigations.pop());

// ------------------------------------------------------------------ 个人资料页
const assess = loadPage('/pages/assess/assess.js');
assess.onSave();
console.log('年龄为空时保存 →', toasts.pop());
assess.setData({ age: '45', stage: 'MID', bendAngle: 100, straightenOk: true });
assess.onSave();
console.log('个人资料保存 =', storage['knee:assessment'].stage, storage['knee:assessment'].age, '｜toast =', toasts.pop());

// ------------------------------------------------------------------ 删除记录
recordsPage.onDeleteRecord({ currentTarget: { dataset: { id: saved.id } } });
recordsPage.onShow();
console.log('删除后条数 =', storage['knee:records'].length);

// ------------------------------------------------------------------ 设置页
const settings = loadPage('/pages/settings/settings.js');
settings.onShow();
console.log('设置页 记录数 =', settings.data.recordCount);
settings.onReminderChange({ detail: { value: true } });
settings.onTimeChange({ detail: { value: '20:30' } });
console.log('设置保存 =', storage['knee:settings']);
settings.onClearAll();
console.log('清除后 =', storage['knee:records'], storage['knee:assessment']);

console.log('\n全部 toast/弹窗：', toasts);
console.log('冒烟测试结束');
