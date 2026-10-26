/**
 * 本地存储层（微信小程序 storage）。
 * 所有页面只允许通过这里读写数据，禁止直接调用 wx.setStorageSync。
 * 本应用只做记录与展示，不保存任何推荐类数据。
 */

const KEY = {
  ASSESSMENT: 'knee:assessment',
  RECORDS: 'knee:records',
  SETTINGS: 'knee:settings',
};

/** 记录条数上限，避免 storage 无限增长 */
const MAX_RECORDS = 200;

function read(key, fallback) {
  try {
    const value = wx.getStorageSync(key);
    if (value === '' || value === null || value === undefined) return fallback;
    return value;
  } catch (e) {
    return fallback;
  }
}

function write(key, value) {
  try {
    wx.setStorageSync(key, value);
  } catch (e) {
    // storage 写入失败不阻断记录流程
  }
}

function remove(key) {
  try {
    wx.removeStorageSync(key);
  } catch (e) {
    // ignore
  }
}

/** YYYY-MM-DD */
function todayKey(ts) {
  const d = ts ? new Date(ts) : new Date();
  const m = `${d.getMonth() + 1}`.padStart(2, '0');
  const day = `${d.getDate()}`.padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
}

/** MM-DD HH:mm */
function formatDateTime(ts) {
  const d = new Date(ts);
  const m = `${d.getMonth() + 1}`.padStart(2, '0');
  const day = `${d.getDate()}`.padStart(2, '0');
  const h = `${d.getHours()}`.padStart(2, '0');
  const min = `${d.getMinutes()}`.padStart(2, '0');
  return `${m}-${day} ${h}:${min}`;
}

// ------------------------------------------------------------------ 基本情况

function getAssessment() {
  const a = read(KEY.ASSESSMENT, null);
  return a && a.stage ? a : null;
}

function saveAssessment(assessment) {
  write(KEY.ASSESSMENT, { ...assessment, updatedAt: Date.now() });
}

// ------------------------------------------------------------------ 锻炼记录

function getRecords() {
  const list = read(KEY.RECORDS, []);
  return Array.isArray(list) ? list : [];
}

function addRecord(record) {
  const list = getRecords();
  list.unshift(record);
  write(KEY.RECORDS, list.slice(0, MAX_RECORDS));
}

function removeRecord(id) {
  write(KEY.RECORDS, getRecords().filter((r) => r.id !== id));
}

// ------------------------------------------------------------------ 设置

const DEFAULT_SETTINGS = {
  reminderEnabled: false,
  reminderTime: '19:00',
};

function getSettings() {
  const s = read(KEY.SETTINGS, null);
  return { ...DEFAULT_SETTINGS, ...(s || {}) };
}

function saveSettings(settings) {
  // 与已有设置合并，避免分次保存时互相覆盖
  write(KEY.SETTINGS, { ...DEFAULT_SETTINGS, ...getSettings(), ...settings });
}

function clearAll() {
  remove(KEY.ASSESSMENT);
  remove(KEY.RECORDS);
  write(KEY.SETTINGS, { ...DEFAULT_SETTINGS });
}

module.exports = {
  KEY,
  todayKey,
  formatDateTime,
  getAssessment,
  saveAssessment,
  getRecords,
  addRecord,
  removeRecord,
  getSettings,
  saveSettings,
  clearAll,
};
