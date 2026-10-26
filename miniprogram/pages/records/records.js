const store = require('../../utils/store.js');

const MAX_CHART_POINTS = 12;

Page({
  data: {
    records: [],
    chartReady: false,
    totalCount: 0,
    totalMin: 0,
  },

  onShow() {
    const list = store.getRecords();
    const records = list.slice(0, 50).map((r) => ({
      ...r,
      dateText: store.formatDateTime(r.timestamp),
      durationText: `${r.durationMin || 0} 分钟`,
      exerciseTexts: (r.exercises || []).map((ex) => (ex.sets ? `${ex.name} ${ex.sets} 组` : ex.name)),
    }));

    const settings = store.getSettings();
    let reminderTip = '';
    if (settings.reminderEnabled) {
      const parts = String(settings.reminderTime).split(':');
      const target = Number(parts[0]) * 60 + Number(parts[1]);
      const now = new Date();
      const current = now.getHours() * 60 + now.getMinutes();
      const today = store.todayKey();
      if (current >= target && !list.some((r) => r.date === today)) {
        reminderTip = `提醒时间是 ${settings.reminderTime}，今天还没有记录。`;
      }
    }

    this.setData({
      records,
      chartReady: list.length > 0,
      totalCount: list.length,
      totalMin: list.reduce((sum, r) => sum + (r.durationMin || 0), 0),
      reminderTip,
    });

    if (list.length > 0) this.drawChart(list.slice(0, MAX_CHART_POINTS).reverse());
  },

  onGoEntry() {
    wx.navigateTo({ url: '/pages/entry/entry' });
  },

  onDeleteRecord(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '删除这条记录',
      content: '删除后无法恢复。',
      confirmText: '删除',
      confirmColor: '#ff3b30',
      success: (res) => {
        if (!res.confirm) return;
        store.removeRecord(id);
        this.onShow();
      },
    });
  },

  drawChart(list) {
    wx.createSelectorQuery()
      .in(this)
      .select('#painChart')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res || !res[0] || !res[0].node) return;
        const canvas = res[0].node;
        const ctx = canvas.getContext('2d');
        const dpr = wx.getSystemInfoSync().pixelRatio || 2;
        const width = res[0].width;
        const height = res[0].height;
        canvas.width = width * dpr;
        canvas.height = height * dpr;
        ctx.scale(dpr, dpr);
        ctx.clearRect(0, 0, width, height);

        const padLeft = 30;
        const padRight = 16;
        const padTop = 18;
        const padBottom = 26;
        const plotW = width - padLeft - padRight;
        const plotH = height - padTop - padBottom;

        // Y 轴：身体感受评分 0-10
        ctx.strokeStyle = '#e8eaed';
        ctx.lineWidth = 1;
        ctx.fillStyle = '#9aa0a6';
        ctx.font = '10px sans-serif';
        for (let v = 0; v <= 10; v += 2) {
          const y = padTop + plotH - (v / 10) * plotH;
          ctx.beginPath();
          ctx.moveTo(padLeft, y);
          ctx.lineTo(padLeft + plotW, y);
          ctx.stroke();
          ctx.fillText(String(v), 6, y + 3);
        }

        const n = list.length;
        const xOf = (i) => (n === 1 ? padLeft + plotW / 2 : padLeft + (plotW * i) / (n - 1));
        const yOf = (v) => padTop + plotH - (v / 10) * plotH;

        ctx.strokeStyle = '#0a84ff';
        ctx.lineWidth = 2;
        ctx.beginPath();
        list.forEach((r, i) => {
          const x = xOf(i);
          const y = yOf(Number(r.feelingScore) || 0);
          if (i === 0) ctx.moveTo(x, y);
          else ctx.lineTo(x, y);
        });
        ctx.stroke();

        ctx.fillStyle = '#0a84ff';
        list.forEach((r, i) => {
          ctx.beginPath();
          ctx.arc(xOf(i), yOf(Number(r.feelingScore) || 0), 3.5, 0, Math.PI * 2);
          ctx.fill();
        });

        ctx.fillStyle = '#9aa0a6';
        list.forEach((r, i) => {
          ctx.fillText(r.date.slice(5), xOf(i) - 12, height - 6);
        });
      });
  },
});
