const store = require('../../utils/store.js');

// 使用须知原文（本应用不提供任何锻炼建议，此处仅为提示性声明）
const NOTICE = {
  text: '我已知晓运动中如有不适立即停止',
  detail: [
    '本应用仅用于个人锻炼记录，不提供任何动作推荐或锻炼建议。',
    '本应用不替代专业人员的判断。',
    '身体持续不适时应停止锻炼，是否寻求专业帮助由你自己决定。',
  ],
};

Page({
  data: {
    reminderEnabled: false,
    reminderTime: '19:00',
    noticeText: NOTICE.text,
    noticeDetail: NOTICE.detail,
    recordCount: 0,
    hasAssessment: false,
  },

  onShow() {
    const settings = store.getSettings();
    this.setData({
      reminderEnabled: settings.reminderEnabled,
      reminderTime: settings.reminderTime,
      recordCount: store.getRecords().length,
      hasAssessment: !!store.getAssessment(),
    });
  },

  onReminderChange(e) {
    const enabled = e.detail.value;
    store.saveSettings({ reminderEnabled: enabled });
    this.setData({ reminderEnabled: enabled });
    wx.showToast({
      title: enabled ? '已开启，打开小程序时会提醒你记录' : '已关闭提醒',
      icon: 'none',
    });
  },

  onTimeChange(e) {
    const time = e.detail.value;
    store.saveSettings({ reminderTime: time });
    this.setData({ reminderTime: time });
  },

  onGoAssess() {
    wx.switchTab({ url: '/pages/assess/assess' });
  },

  onClearAll() {
    wx.showModal({
      title: '清除全部数据',
      content: '将删除全部锻炼记录与个人资料，且无法恢复。',
      confirmText: '确认清除',
      confirmColor: '#ff3b30',
      success: (res) => {
        if (!res.confirm) return;
        store.clearAll();
        this.setData({
          recordCount: 0,
          hasAssessment: false,
          reminderEnabled: false,
          reminderTime: '19:00',
        });
        wx.showToast({ title: '已清除', icon: 'none' });
      },
    });
  },
});
